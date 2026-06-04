package com.bookd.app.data.vm

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bookd.app.Database
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.NetworkAddressProvider
import com.bookd.app.data.api.ReaderApi
import com.bookd.app.data.model.BookManifest
import com.bookd.app.data.model.BookMetadata
import com.bookd.app.data.model.BookmarkDTO
import com.bookd.app.data.model.BookmarkResponse
import com.bookd.app.data.model.BookmarksResponse
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.LocalReadingProgress
import com.bookd.app.data.model.ReaderSettingsDTO
import com.bookd.app.data.model.ReadingProgressDTO
import com.bookd.app.data.model.ReadingProgressResponse
import com.bookd.app.data.model.TextSpan
import com.bookd.app.data.model.TocItem
import com.bookd.app.data.repository.ReaderRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReaderViewModelNavigationTest {

    private lateinit var database: Database
    private lateinit var fakeApi: ReaderNavigationFakeApi
    private lateinit var repository: ReaderRepository
    private lateinit var viewModel: ReaderViewModel

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
        fakeApi = ReaderNavigationFakeApi()
        repository = ReaderRepository(database, ReaderNavigationFakeApiProvider(fakeApi))
        viewModel = ReaderViewModel(repository)
    }

    @Test
    fun `given saved local anchor progress when loadBook then emits anchor-first scroll request`() = runBlocking {
        repository.saveLocalProgress(
            LocalReadingProgress(
                bookId = 1,
                chapterIndex = 2,
                anchorId = "ch2-p1",
                paragraphIndex = 1,
                scrollOffset = 96,
                pageIndex = 0,
                progress = 0.25,
                lastReadAt = 1000L
            )
        )

        val effect = async {
            viewModel.effect
                .filter { it is ReaderEffect.ScrollToPosition }
                .first() as ReaderEffect.ScrollToPosition
        }
        delay(10)

        viewModel.loadBook(bookId = 1)

        val scroll = withTimeout(3000) { effect.await() }
        assertEquals(2, scroll.chapterIndex)
        assertEquals("ch2-p1", scroll.anchorId)
        assertEquals(1, scroll.paragraphIndex)
        assertEquals(96, scroll.offset)
        assertTrue(viewModel.state.value.isProgrammaticJumpPending)
    }

    @Test
    fun `given loaded reader when jumpToChapter then emits top-of-chapter scroll request`() = runBlocking {
        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading && viewModel.state.value.currentChapter != null }

        val effect = async {
            viewModel.effect
                .filter { it is ReaderEffect.ScrollToPosition }
                .first() as ReaderEffect.ScrollToPosition
        }
        delay(10)

        viewModel.jumpToChapter(3)

        val scroll = withTimeout(3000) { effect.await() }
        assertEquals(3, scroll.chapterIndex)
        assertEquals(null, scroll.anchorId)
        assertEquals(0, scroll.paragraphIndex)
        assertEquals(0, scroll.offset)
    }

    @Test
    fun `given internal link target when jumpToInternalLink then emits anchor scroll request`() = runBlocking {
        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading && viewModel.state.value.currentChapter != null }

        val effect = async {
            viewModel.effect
                .filter { it is ReaderEffect.ScrollToPosition }
                .first() as ReaderEffect.ScrollToPosition
        }
        delay(10)

        viewModel.jumpToInternalLink(chapterIndex = 2, anchorId = "ch2-p1")

        val scroll = withTimeout(3000) { effect.await() }
        assertEquals(2, scroll.chapterIndex)
        assertEquals("ch2-p1", scroll.anchorId)
        assertEquals(0, scroll.paragraphIndex)
        assertEquals(0, scroll.offset)
    }

    @Test
    fun `given current anchor position when addBookmarkAtCurrentPosition then appends returned bookmark`() = runBlocking {
        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading && viewModel.state.value.currentChapter != null }
        viewModel.onProgrammaticScrollCompleted(
            chapterIndex = 1,
            anchorId = "ch1-p2",
            paragraphIndex = 2,
            scrollOffset = 48,
        )

        viewModel.addBookmarkAtCurrentPosition(note = "标记")

        waitUntil { viewModel.state.value.bookmarks.isNotEmpty() }
        val bookmark = viewModel.state.value.bookmarks.single()
        assertEquals(1, bookmark.chapterIndex)
        assertEquals("ch1-p2", bookmark.anchorId)
        assertEquals(2, bookmark.paragraphIndex)
        assertEquals(48, bookmark.scrollOffset)
        assertEquals("标记", bookmark.note)
        assertNotNull(fakeApi.lastBookmarkDto)
        assertEquals("ch1-p2", fakeApi.lastBookmarkDto?.anchorId)
    }

    private suspend fun waitUntil(condition: () -> Boolean) {
        withTimeout(3000) {
            while (!condition()) {
                delay(10)
            }
        }
    }
}

private class ReaderNavigationFakeApi : ReaderApi {
    var lastBookmarkDto: BookmarkDTO? = null

    override suspend fun getBookManifest(bookId: Int): BookManifest = manifest(bookId)

    override suspend fun getBookManifestWithProgress(bookId: Int): BookManifest = manifest(bookId)

    override suspend fun getChapterContent(bookId: Int, chapterIndex: Int): ChapterContent {
        return ChapterContent(
            index = chapterIndex,
            title = "第${chapterIndex + 1}章",
            elements = listOf(
                ContentElement.Paragraph(
                    spans = listOf(TextSpan(text = "第一段")),
                    anchorId = "ch$chapterIndex-p0",
                ),
                ContentElement.Paragraph(
                    spans = listOf(TextSpan(text = "第二段")),
                    anchorId = "ch$chapterIndex-p1",
                ),
                ContentElement.Paragraph(
                    spans = listOf(TextSpan(text = "第三段")),
                    anchorId = "ch$chapterIndex-p2",
                ),
            ),
            prevIndex = (chapterIndex - 1).takeIf { it >= 0 },
            nextIndex = (chapterIndex + 1).takeIf { it < 5 },
        )
    }

    override suspend fun getReadingProgress(bookId: Int): ReadingProgressResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun updateReadingProgress(
        bookId: Int,
        progress: ReadingProgressDTO,
    ): ReadingProgressResponse {
        return ReadingProgressResponse(
            id = 1,
            bookId = bookId,
            progress = progress.progress,
            currentPage = progress.currentPage ?: 0,
            lastReadAt = "2026-06-03T00:00:00Z",
            chapterIndex = progress.chapterIndex ?: progress.currentPage ?: 0,
            anchorId = progress.anchorId,
            paragraphIndex = progress.paragraphIndex,
            scrollOffset = progress.scrollOffset,
        )
    }

    override suspend fun getBookmarks(bookId: Int): BookmarksResponse {
        return BookmarksResponse(bookmarks = emptyList(), total = 0)
    }

    override suspend fun addBookmark(bookId: Int, bookmark: BookmarkDTO): BookmarkResponse {
        lastBookmarkDto = bookmark
        return BookmarkResponse(
            id = 1,
            bookId = bookId,
            chapterIndex = bookmark.chapterIndex,
            anchorId = bookmark.anchorId,
            paragraphIndex = bookmark.paragraphIndex,
            scrollOffset = bookmark.scrollOffset,
            note = bookmark.note,
            createdAt = "2026-06-03T00:00:00Z",
        )
    }

    override suspend fun updateBookmark(bookmarkId: Int, bookmark: BookmarkDTO): BookmarkResponse {
        throw UnsupportedOperationException()
    }

    override suspend fun deleteBookmark(bookmarkId: Int) {
    }

    override suspend fun getReaderSettings(): ReaderSettingsDTO = ReaderSettingsDTO()

    override suspend fun updateReaderSettings(settings: ReaderSettingsDTO): ReaderSettingsDTO = settings

    override suspend fun patchReaderSettings(settings: ReaderSettingsDTO): ReaderSettingsDTO = settings

    private fun manifest(bookId: Int): BookManifest {
        return BookManifest(
            id = bookId,
            title = "测试书",
            author = "作者",
            format = "epub",
            totalChapters = 5,
            toc = (0 until 5).map { TocItem(index = it, title = "第${it + 1}章", wordCount = 100) },
            spine = (0 until 5).toList(),
            metadata = BookMetadata(),
        )
    }
}

private class ReaderNavigationFakeApiProvider(
    private val api: ReaderApi,
) : ApiProvider(
    httpClient = io.ktor.client.HttpClient(),
    networkSwitcher = object : NetworkAddressProvider {
        override val currentUrl: String? = null
    },
) {
    override fun getReaderApiOrNull(): ReaderApi = api
}
