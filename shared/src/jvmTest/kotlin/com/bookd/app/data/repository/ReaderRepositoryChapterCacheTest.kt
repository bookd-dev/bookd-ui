package com.bookd.app.data.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bookd.app.Database
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.NetworkAddressProvider
import com.bookd.app.data.api.NoNetworkConfigException
import com.bookd.app.data.api.ReaderApi
import com.bookd.app.data.model.BookManifest
import com.bookd.app.data.model.BookmarkDTO
import com.bookd.app.data.model.BookmarkResponse
import com.bookd.app.data.model.BookmarksResponse
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettingsDTO
import com.bookd.app.data.model.ReadingProgressDTO
import com.bookd.app.data.model.ReadingProgressResponse
import com.bookd.app.data.model.TextSpan
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ReaderRepository.getChapterContent() 章节缓存逻辑单元测试
 *
 * 验证核心场景：
 * 1. 缓存 elements 非空时直接命中，不重新请求网络
 * 2. 缓存 elements 为空（历史脏数据）时删除缓存并重新从网络获取
 * 3. 缓存不存在时从网络获取并写入缓存
 * 4. 未配置网络时返回 Failure
 */
class ReaderRepositoryChapterCacheTest {

    // ============ 测试基础设施 ============

    private lateinit var database: Database
    private lateinit var fakeApi: FakeReaderApi
    private lateinit var repository: ReaderRepository

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Before
    fun setUp() {
        // 使用内存 SQLite，每次测试独立创建，互不影响
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)

        fakeApi = FakeReaderApi()
        repository = ReaderRepository(database, FakeApiProvider(fakeApi))
    }

    // ============ 辅助构造函数 ============

    /** 构造有内容的 ChapterContent */
    private fun chapterWithElements(
        index: Int = 1,
        title: String = "第一章",
        elementCount: Int = 3
    ) = ChapterContent(
        index = index,
        title = title,
        elements = List(elementCount) { i ->
            ContentElement.Paragraph(spans = listOf(TextSpan(text = "段落 $i")))
        },
        prevIndex = if (index > 0) index - 1 else null,
        nextIndex = index + 1
    )

    /** 构造 elements 为空的 ChapterContent（历史脏数据场景） */
    private fun chapterWithEmptyElements(index: Int = 1, title: String = "第一章") =
        ChapterContent(
            index = index,
            title = title,
            elements = emptyList(),
            prevIndex = if (index > 0) index - 1 else null,
            nextIndex = index + 1
        )

    /** 将 ChapterContent 直接写入 SQLite 缓存（模拟历史缓存数据） */
    private fun insertCacheDirectly(bookId: Int, chapter: ChapterContent) = runBlocking {
        database.chapterCacheQueries.insertOrReplace(
            bookId = bookId.toLong(),
            chapterIndex = chapter.index.toLong(),
            content = json.encodeToString(chapter),
            cachedAt = 0L
        )
    }

    /** 查询 SQLite 中是否有该章节的缓存记录 */
    private fun hasCacheRecord(bookId: Int, chapterIndex: Int): Boolean =
        runBlocking {
            database.chapterCacheQueries
                .selectByBookAndChapter(bookId.toLong(), chapterIndex.toLong())
                .executeAsOneOrNull() != null
        }

    // ============ 测试用例 ============

    @Test
    fun `given valid cache with elements when getChapterContent then returns cached data without network call`() {
        val bookId = 1
        val chapter = chapterWithElements(index = 1, elementCount = 5)
        insertCacheDirectly(bookId, chapter)
        fakeApi.networkCallCount = 0

        val result = runBlocking { repository.getChapterContent(bookId, chapter.index) }

        assertTrue(result.isSuccess, "应返回 Success")
        assertEquals(5, result.getOrThrow().elements.size, "应返回缓存中的 5 个 elements")
        assertEquals(0, fakeApi.networkCallCount, "缓存命中时不应发起网络请求")
    }

    @Test
    fun `given cache with empty elements when getChapterContent then fetches fresh data from network`() {
        val bookId = 2
        val staleChapter = chapterWithEmptyElements(index = 3)
        insertCacheDirectly(bookId, staleChapter)

        // 网络会返回有内容的章节
        val freshChapter = chapterWithElements(index = 3, elementCount = 10)
        fakeApi.chapterToReturn = freshChapter

        val result = runBlocking { repository.getChapterContent(bookId, staleChapter.index) }

        assertTrue(result.isSuccess, "应返回 Success")
        assertEquals(10, result.getOrThrow().elements.size, "应返回网络获取的 10 个 elements")
        assertEquals(1, fakeApi.networkCallCount, "空缓存应触发一次网络请求")
    }

    @Test
    fun `given cache with empty elements when getChapterContent then stale cache is replaced with fresh data`() {
        val bookId = 3
        val staleChapter = chapterWithEmptyElements(index = 1)
        insertCacheDirectly(bookId, staleChapter)

        val freshChapter = chapterWithElements(index = 1, elementCount = 7)
        fakeApi.chapterToReturn = freshChapter

        runBlocking { repository.getChapterContent(bookId, staleChapter.index) }

        // 验证缓存已被新数据替换
        val cachedRaw = runBlocking {
            database.chapterCacheQueries
                .selectByBookAndChapter(bookId.toLong(), 1L)
                .executeAsOneOrNull()
        }
        assertNotNull(cachedRaw, "网络获取后应将新数据写入缓存")
        val cached = json.decodeFromString<ChapterContent>(cachedRaw.content)
        assertEquals(7, cached.elements.size, "缓存中应存储新获取的 7 个 elements")
    }

    @Test
    fun `given no cache when getChapterContent then fetches from network and writes to cache`() {
        val bookId = 4
        val chapterIndex = 2
        val freshChapter = chapterWithElements(index = chapterIndex, elementCount = 6)
        fakeApi.chapterToReturn = freshChapter

        val result = runBlocking { repository.getChapterContent(bookId, chapterIndex) }

        assertTrue(result.isSuccess, "无缓存时应成功返回网络数据")
        assertEquals(6, result.getOrThrow().elements.size)
        assertEquals(1, fakeApi.networkCallCount, "无缓存时应发起一次网络请求")
        assertTrue(hasCacheRecord(bookId, chapterIndex), "网络数据应被写入缓存")
    }

    @Test
    fun `given no network configured and no cache when getChapterContent then returns failure`() {
        val bookId = 5
        repository = ReaderRepository(database, FakeApiProvider(fakeApi, networkConfigured = false))

        val result = runBlocking { repository.getChapterContent(bookId, 1) }

        assertTrue(result.isFailure, "未配置网络时应返回 Failure")
        assertTrue(result.exceptionOrNull() is NoNetworkConfigException)
    }

    @Test
    fun `given cache with empty elements and no network when getChapterContent then returns failure`() {
        val bookId = 6
        val staleChapter = chapterWithEmptyElements(index = 1)
        insertCacheDirectly(bookId, staleChapter)
        // 空缓存视为无效，但网络未配置
        repository = ReaderRepository(database, FakeApiProvider(fakeApi, networkConfigured = false))

        val result = runBlocking { repository.getChapterContent(bookId, staleChapter.index) }

        assertTrue(result.isFailure, "空缓存 + 无网络时应返回 Failure")
        assertTrue(result.exceptionOrNull() is NoNetworkConfigException)
    }
}

// ============ 测试辅助：Fake 实现 ============

/**
 * 可控的 ReaderApi fake 实现
 * 通过属性控制返回值和调用计数，不依赖 MockK
 */
private class FakeReaderApi : ReaderApi {
    var chapterToReturn: ChapterContent = ChapterContent(
        index = 0, title = null, elements = emptyList(), prevIndex = null, nextIndex = null
    )
    var networkCallCount = 0

    override suspend fun getChapterContent(bookId: Int, chapterIndex: Int): ChapterContent {
        networkCallCount++
        return chapterToReturn
    }

    override suspend fun getBookManifest(bookId: Int): BookManifest =
        throw UnsupportedOperationException()

    override suspend fun getBookManifestWithProgress(bookId: Int): BookManifest =
        throw UnsupportedOperationException()

    override suspend fun getReadingProgress(bookId: Int): ReadingProgressResponse =
        throw UnsupportedOperationException()

    override suspend fun updateReadingProgress(
        bookId: Int, progress: ReadingProgressDTO
    ): ReadingProgressResponse = throw UnsupportedOperationException()

    override suspend fun getBookmarks(bookId: Int): BookmarksResponse =
        throw UnsupportedOperationException()

    override suspend fun addBookmark(bookId: Int, bookmark: BookmarkDTO): BookmarkResponse =
        throw UnsupportedOperationException()

    override suspend fun updateBookmark(bookmarkId: Int, bookmark: BookmarkDTO): BookmarkResponse =
        throw UnsupportedOperationException()

    override suspend fun deleteBookmark(bookmarkId: Int): Unit =
        throw UnsupportedOperationException()

    override suspend fun getReaderSettings(): ReaderSettingsDTO =
        throw UnsupportedOperationException()

    override suspend fun updateReaderSettings(settings: ReaderSettingsDTO): ReaderSettingsDTO =
        throw UnsupportedOperationException()

    override suspend fun patchReaderSettings(settings: ReaderSettingsDTO): ReaderSettingsDTO =
        throw UnsupportedOperationException()
}

/**
 * 可控的 ApiProvider fake 实现
 * 继承 ApiProvider，override getReaderApiOrNull() 直接返回 FakeReaderApi，
 * 无需真实 HttpClient 或 NetworkSwitcher。
 * networkConfigured=false 时返回 null，模拟未配置网络
 */
private class FakeApiProvider(
    private val api: ReaderApi,
    val networkConfigured: Boolean = true
) : ApiProvider(
    httpClient = io.ktor.client.HttpClient(),
    networkSwitcher = object : NetworkAddressProvider {
        override val currentUrl: String? = null // 占位；实际由 getReaderApiOrNull() 控制
    }
) {
    override fun getReaderApiOrNull(): ReaderApi? = if (networkConfigured) api else null
}
