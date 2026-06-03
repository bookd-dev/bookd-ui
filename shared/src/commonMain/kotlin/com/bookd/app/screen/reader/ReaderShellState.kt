package com.bookd.app.screen.reader

import com.bookd.app.data.vm.ReaderState

data class ReaderShellUiState(
    val showContent: Boolean,
    val showBlockingLoading: Boolean,
    val showInlineLoading: Boolean,
    val showBlockingError: Boolean,
    val showProgressConflict: Boolean,
)

fun buildReaderShellUiState(state: ReaderState): ReaderShellUiState {
    val hasContent = !state.isLoading && state.currentChapter != null
    return ReaderShellUiState(
        showContent = hasContent,
        showBlockingLoading = state.isLoading || (state.isLoadingChapter && state.currentChapter == null),
        showInlineLoading = state.isLoadingChapter && state.currentChapter != null,
        showBlockingError = state.error != null && state.currentChapter == null,
        showProgressConflict = state.hasProgressConflict && state.localProgress != null && state.remoteProgress != null,
    )
}
