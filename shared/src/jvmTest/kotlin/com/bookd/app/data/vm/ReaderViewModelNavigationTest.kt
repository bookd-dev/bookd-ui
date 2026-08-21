@file:OptIn(com.russhwolf.settings.ExperimentalSettingsImplementation::class)

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
import com.russhwolf.settings.PropertiesSettings
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import java.util.Properties
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReaderViewModelNavigationTest {

    private lateinit var database: Database
    private lateinit var fakeApi: ReaderNavigationFakeApi
    private lateinit var repository: ReaderRepository
    private lateinit var viewModel: ReaderViewModel
    private lateinit var settingsStore: PropertiesSettings

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
        fakeApi = ReaderNavigationFakeApi()
        settingsStore = PropertiesSettings(Properties())
        repository = ReaderRepository(database, ReaderNavigationFakeApiProvider(fakeApi), settingsStore)
        viewModel = ReaderViewModel(repository)
    }

    @Test
    fun `given font size changed when reader exits before debounce then reopened reader restores local value`() = runBlocking {
        viewModel.updateFontSize(24)

        val reopenedRepository = ReaderRepository(
            database,
            ReaderNavigationFakeApiProvider(fakeApi),
            settingsStore,
        )

        assertEquals(24, reopenedRepository.getReaderSettings().fontSize)
        assertTrue(reopenedRepository.hasPendingReaderSettingsSync())
    }

    @Test
    fun `given saved local anchor progress when loadBook then exposes durable anchor-first scroll request`() = runBlocking {
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

        viewModel.loadBook(bookId = 1)
        waitUntil { viewModel.state.value.pendingPositionRequest != null }

        val scroll = requireNotNull(viewModel.state.value.pendingPositionRequest)
        assertEquals(2, scroll.chapterIndex)
        assertEquals("ch2-p1", scroll.anchorId)
        assertEquals(1, scroll.paragraphIndex)
        assertEquals(96, scroll.offset)
        assertTrue(viewModel.state.value.isProgrammaticJumpPending)
    }

    @Test
    fun `given saved second page when reader reopens then ignores initial pager callbacks until exact restore completes`() = runBlocking {
        repository.saveLocalProgress(
            LocalReadingProgress(
                bookId = 1,
                chapterIndex = 2,
                anchorId = "ch2-p1",
                paragraphIndex = 1,
                scrollOffset = 0,
                pageIndex = 1,
                progress = 0.25,
                lastReadAt = 1000L,
            )
        )

        viewModel.loadBook(bookId = 1)
        waitUntil {
            !viewModel.state.value.isLoading &&
                viewModel.state.value.currentChapter != null &&
                viewModel.state.value.pendingPositionRequest != null
        }

        val request = requireNotNull(viewModel.state.value.pendingPositionRequest)
        assertEquals(2, request.chapterIndex)
        assertEquals(1, request.pageIndex)

        viewModel.updatePagePosition(2, 0, "ch2-p0", 0)
        viewModel.onPagerChapterChanged(0, -1)

        assertEquals(2, viewModel.state.value.currentChapterIndex)
        assertEquals(1, viewModel.state.value.currentPageIndex)

        viewModel.onProgrammaticScrollCompleted(
            chapterIndex = 2,
            anchorId = "ch2-p1",
            paragraphIndex = 1,
            scrollOffset = 0,
            pageIndex = 1,
            sequence = request.sequence,
        )

        assertEquals(2, viewModel.state.value.currentChapterIndex)
        assertEquals(1, viewModel.state.value.currentPageIndex)
        assertEquals("ch2-p1", viewModel.state.value.currentAnchorId)
        assertFalse(viewModel.state.value.isProgrammaticJumpPending)
        assertEquals(null, viewModel.state.value.pendingPositionRequest)
    }

    @Test
    fun `given loaded reader when jumpToChapter then exposes top-of-chapter scroll request`() = runBlocking {
        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading && viewModel.state.value.currentChapter != null }

        viewModel.jumpToChapter(3)

        val scroll = requireNotNull(viewModel.state.value.pendingPositionRequest)
        assertEquals(3, scroll.chapterIndex)
        assertEquals(null, scroll.anchorId)
        assertEquals(0, scroll.paragraphIndex)
        assertEquals(0, scroll.offset)
    }

    @Test
    fun `given internal link target when jumpToInternalLink then exposes durable anchor scroll request`() = runBlocking {
        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading && viewModel.state.value.currentChapter != null }

        viewModel.jumpToInternalLink(chapterIndex = 2, anchorId = "ch2-p1")

        val scroll = requireNotNull(viewModel.state.value.pendingPositionRequest)
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

    @Test
    fun `given small same chapter position change when reader settles then syncs without percentage threshold`() = runBlocking {
        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading && viewModel.state.value.currentChapter != null }

        viewModel.updateScrollPosition(
            chapterIndex = 0,
            anchorId = "ch0-p0",
            paragraphIndex = 0,
            scrollOffset = 12,
        )

        withTimeout(300) {
            while (repository.getLocalProgress(1)?.scrollOffset != 12) delay(10)
        }
        withTimeout(1500) {
            while (fakeApi.progressUpdates.isEmpty()) delay(10)
        }
        val update = fakeApi.progressUpdates.single()
        assertEquals("ch0-p0", update.anchorId)
        assertEquals(0, update.paragraphIndex)
        assertEquals(12, update.scrollOffset)
        assertTrue(update.progress < 0.05)
    }

    @Test
    fun `given newer positions during active sync when request completes then uploads only latest pending snapshot`() = runBlocking {
        fakeApi.progressUpdateDelayMs = 250L
        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading && viewModel.state.value.currentChapter != null }

        viewModel.updateScrollPosition(1, "ch1-p0", 0, 0)
        withTimeout(300) {
            while (fakeApi.progressUpdates.isEmpty()) delay(10)
        }
        viewModel.updateScrollPosition(1, "ch1-p1", 1, 0)
        viewModel.updateScrollPosition(1, "ch1-p2", 2, 0)

        withTimeout(1500) {
            while (fakeApi.progressUpdates.size < 2) delay(10)
        }
        delay(700)
        assertEquals(2, fakeApi.progressUpdates.size)
        assertEquals("ch1-p2", fakeApi.progressUpdates.last().anchorId)
    }

    @Test
    fun `given local and remote anchors differ in same chapter when load then requires explicit conflict choice`() = runBlocking {
        repository.saveLocalProgress(
            LocalReadingProgress(
                bookId = 1,
                chapterIndex = 2,
                anchorId = "ch2-p1",
                paragraphIndex = 1,
                scrollOffset = 0,
                pageIndex = 0,
                progress = 0.4,
                lastReadAt = 1000L,
            )
        )
        fakeApi.remoteProgress = ReadingProgressResponse(
            id = 1,
            bookId = 1,
            progress = 0.4,
            currentPage = 2,
            lastReadAt = "2026-06-03T00:00:00Z",
            chapterIndex = 2,
            anchorId = "ch2-p2",
            paragraphIndex = 2,
            scrollOffset = 0,
            chapterPageIndex = 0,
        )

        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading }

        assertTrue(viewModel.state.value.hasProgressConflict)
        assertEquals("ch2-p1", viewModel.state.value.localProgress?.anchorId)
        assertEquals("ch2-p2", viewModel.state.value.remoteProgress?.anchorId)
    }

    @Test
    fun `given local and remote coordinates match when percentages differ then does not report conflict`() = runBlocking {
        repository.saveLocalProgress(
            LocalReadingProgress(
                bookId = 1,
                chapterIndex = 2,
                anchorId = "ch2-p1",
                paragraphIndex = 1,
                scrollOffset = 8,
                pageIndex = 0,
                progress = 0.4,
                lastReadAt = 1000L,
            )
        )
        fakeApi.remoteProgress = ReadingProgressResponse(
            id = 1,
            bookId = 1,
            progress = 0.45,
            currentPage = 2,
            lastReadAt = "2026-06-03T00:00:00Z",
            chapterIndex = 2,
            anchorId = "ch2-p1",
            paragraphIndex = 1,
            scrollOffset = 8,
            chapterPageIndex = 0,
        )

        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading && viewModel.state.value.currentChapter != null }

        assertFalse(viewModel.state.value.hasProgressConflict)
    }

    @Test
    fun `given pending debounced progress when user exits then flushes exact position before navigation`() = runBlocking {
        viewModel.loadBook(bookId = 1)
        waitUntil { !viewModel.state.value.isLoading && viewModel.state.value.currentChapter != null }
        viewModel.updateScrollPosition(0, "ch0-p2", 2, 24)

        val navigation = async {
            viewModel.effect.filter { it is ReaderEffect.NavigateBack }.first()
        }
        delay(10)
        viewModel.back()
        withTimeout(1500) { navigation.await() }

        assertFalse(fakeApi.progressUpdates.isEmpty())
        val update = fakeApi.progressUpdates.last()
        assertEquals("ch0-p2", update.anchorId)
        assertEquals(2, update.paragraphIndex)
        assertEquals(24, update.scrollOffset)
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
    var remoteProgress: ReadingProgressResponse? = null
    var progressUpdateDelayMs: Long = 0L
    val progressUpdates = mutableListOf<ReadingProgressDTO>()

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
        return remoteProgress ?: throw UnsupportedOperationException()
    }

    override suspend fun updateReadingProgress(
        bookId: Int,
        progress: ReadingProgressDTO,
    ): ReadingProgressResponse {
        progressUpdates += progress
        if (progressUpdateDelayMs > 0) delay(progressUpdateDelayMs)
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
