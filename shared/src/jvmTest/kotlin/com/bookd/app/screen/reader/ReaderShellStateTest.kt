package com.bookd.app.screen.reader

import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.LocalReadingProgress
import com.bookd.app.data.model.ReadingProgressResponse
import com.bookd.app.data.model.TextSpan
import com.bookd.app.data.vm.ReaderState
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderShellStateTest {

    @Test
    fun `given initial loading state when build shell state then shows blocking loading`() {
        val shell = buildReaderShellUiState(ReaderState(isLoading = true))

        assertTrue(shell.showBlockingLoading)
        assertFalse(shell.showContent)
        assertFalse(shell.showProgressConflict)
    }

    @Test
    fun `given load error without content when build shell state then shows blocking error`() {
        val shell = buildReaderShellUiState(
            ReaderState(
                isLoading = false,
                error = "failed"
            )
        )

        assertTrue(shell.showBlockingError)
        assertFalse(shell.showContent)
    }

    @Test
    fun `given content and chapter loading when build shell state then shows content and inline loading`() {
        val shell = buildReaderShellUiState(
            ReaderState(
                isLoading = false,
                isLoadingChapter = true,
                currentChapter = chapter()
            )
        )

        assertTrue(shell.showContent)
        assertTrue(shell.showInlineLoading)
        assertFalse(shell.showBlockingLoading)
    }

    @Test
    fun `given progress conflict with both sources when build shell state then requires explicit choice`() {
        val shell = buildReaderShellUiState(
            ReaderState(
                isLoading = false,
                hasProgressConflict = true,
                localProgress = localProgress(),
                remoteProgress = remoteProgress()
            )
        )

        assertTrue(shell.showProgressConflict)
        assertFalse(shell.showContent)
    }

    @Test
    fun `given conflict flag without both progress sources when build shell state then does not show conflict dialog`() {
        val shell = buildReaderShellUiState(
            ReaderState(
                isLoading = false,
                hasProgressConflict = true,
                localProgress = localProgress(),
                remoteProgress = null
            )
        )

        assertFalse(shell.showProgressConflict)
    }

    private fun chapter() = ChapterContent(
        index = 0,
        title = "Chapter 1",
        elements = listOf(ContentElement.Paragraph(listOf(TextSpan("content")))),
        prevIndex = null,
        nextIndex = 1
    )

    private fun localProgress() = LocalReadingProgress(
        bookId = 1,
        chapterIndex = 2,
        paragraphIndex = 3,
        scrollOffset = 0,
        pageIndex = 0,
        progress = 0.4,
        lastReadAt = 1_000L
    )

    private fun remoteProgress() = ReadingProgressResponse(
        id = 1,
        bookId = 1,
        progress = 0.7,
        currentPage = 5,
        totalPages = 10,
        lastReadAt = "2026-06-03T00:00:00Z"
    )
}
