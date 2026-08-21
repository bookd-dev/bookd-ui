package com.bookd.app.screen.reader

import com.bookd.app.data.vm.ReaderState

data class ReaderShellUiState(
    val showContent: Boolean,
    val showBlockingLoading: Boolean,
    val showInlineLoading: Boolean,
    val showBlockingError: Boolean,
    val showProgressConflict: Boolean,
    val footerProgressText: String?,
)

fun buildReaderShellUiState(state: ReaderState): ReaderShellUiState {
    val hasContent = !state.isLoading && state.currentChapter != null
    return ReaderShellUiState(
        showContent = hasContent,
        showBlockingLoading = state.isLoading || (state.isLoadingChapter && state.currentChapter == null),
        showInlineLoading = state.isLoadingChapter && state.currentChapter != null,
        showBlockingError = state.error != null && state.currentChapter == null,
        showProgressConflict = state.hasProgressConflict && state.localProgress != null && state.remoteProgress != null,
        footerProgressText = buildReaderFooterProgressText(state.currentChapterIndex, state.chapterCount),
    )
}

fun buildReaderFooterProgressText(currentChapterIndex: Int, totalChapters: Int): String? {
    if (totalChapters <= 0) return null
    val current = (currentChapterIndex + 1).coerceIn(1, totalChapters)
    return "$current/$totalChapters"
}
