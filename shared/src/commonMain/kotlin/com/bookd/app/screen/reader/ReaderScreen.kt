package com.bookd.app.screen.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bookd.app.basic.extension.logD
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.app_name
import app.composeapp.generated.resources.bookmark_added
import app.composeapp.generated.resources.bookmark_deleted
import app.composeapp.generated.resources.progress_saved
import app.composeapp.generated.resources.reader_loading_chapter
import com.bookd.app.basic.extension.getCurrentTimeString
import com.bookd.app.data.model.PageMode
import com.bookd.app.data.vm.ReaderEffect
import com.bookd.app.data.vm.ReaderViewModel
import com.bookd.app.screen.RouteBookDetail
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.screen.reader.component.ReaderBottomMenu
import com.bookd.app.screen.reader.component.ReaderErrorSurface
import com.bookd.app.screen.reader.component.ReaderLoadingSurface
import com.bookd.app.screen.reader.component.ReaderProgressConflictDialog
import com.bookd.app.screen.reader.component.ReaderSettingsSheet
import com.bookd.app.screen.reader.component.ReaderStatusBar
import com.bookd.app.screen.reader.component.ReaderTocSheet
import com.bookd.app.screen.reader.component.ReaderTopChrome
import com.bookd.app.screen.reader.content.ReaderScrollRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource

/**
 * 阅读器页面
 *
 * @param bookId 书籍 ID
 * @param startChapterIndex 起始章节索引（可选，用于从目录跳转）
 */
@Composable
fun ReaderScreen(
    bookId: Int,
    startChapterIndex: Int? = null
) {
    val screenContext = rememberScreenContext<ReaderViewModel>()
    val viewModel = screenContext.viewModel
    val navigator = screenContext.navigator
    val snackbarHostState = screenContext.snackbarHostState

    val state by viewModel.state.collectAsState()
    val currentPageMode by rememberUpdatedState(state.readerSettings.pageMode)
    var isChromeVisible by remember { mutableStateOf(true) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var isTocVisible by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }
    var pendingScrollRequest by remember { mutableStateOf<ReaderScrollRequest?>(null) }
    val shellUiState = buildReaderShellUiState(state)

    // 预加载国际化字符串
    val fallbackTitle = stringResource(Res.string.app_name)
    val bookmarkAddedMsg = stringResource(Res.string.bookmark_added)
    val bookmarkDeletedMsg = stringResource(Res.string.bookmark_deleted)
    val progressSavedMsg = stringResource(Res.string.progress_saved)
    val footerProgressText = shellUiState.footerProgressText ?: stringResource(Res.string.reader_loading_chapter)

    // 初始加载
    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId, startChapterIndex)
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = getCurrentTimeString()
            delay(60_000L)
        }
    }

    // 处理一次性效果
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ReaderEffect.NavigateBack -> {
                    navigator.navigateBack()
                }
                is ReaderEffect.NavigateToBookDetail -> {
                    navigator.navigateTo(RouteBookDetail(effect.bookId))
                }
                is ReaderEffect.BookmarkAdded -> {
                    snackbarHostState.showSnackbar(bookmarkAddedMsg)
                }
                is ReaderEffect.BookmarkDeleted -> {
                    snackbarHostState.showSnackbar(bookmarkDeletedMsg)
                }
                is ReaderEffect.ProgressSaved -> {
                    snackbarHostState.showSnackbar(progressSavedMsg)
                }
                is ReaderEffect.ScrollToPosition -> {
                    pendingScrollRequest = ReaderScrollRequest(
                        sequence = effect.sequence,
                        chapterIndex = effect.chapterIndex,
                        anchorId = effect.anchorId,
                        paragraphIndex = effect.paragraphIndex,
                        offset = effect.offset
                    )
                    if (currentPageMode != PageMode.SCROLL) {
                        viewModel.onProgrammaticScrollCompleted(
                            chapterIndex = effect.chapterIndex,
                            anchorId = effect.anchorId,
                            paragraphIndex = effect.paragraphIndex,
                            scrollOffset = effect.offset
                        )
                        pendingScrollRequest = null
                    }
                }
                is ReaderEffect.ScrollToPage -> {
                    // 翻页模式跳页（翻页模式实现时处理）
                }
            }
        }
    }

    val currentChapter = state.currentChapter
    val adjacentChapters = state.adjacentChapters

    // [日志] 每次重组时打印关键 state，帮助排查空白页
    logD(tag = "Reader") {
        "[ReaderScreen] 重组 bookId=${state.bookId}" +
        " isLoading=${state.isLoading}" +
        " isLoadingChapter=${state.isLoadingChapter}" +
        " currentChapterIndex=${state.currentChapterIndex}" +
        " currentChapter=${if (currentChapter != null) "已加载(${currentChapter.elements.size}elems)" else "null"}" +
        " adjacentChapters=${adjacentChapters.keys.sorted()}" +
        " hasProgressConflict=${state.hasProgressConflict}" +
        " error=${state.error}"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            shellUiState.showContent && currentChapter != null -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        ReaderContent(
                            bookId = state.bookId,
                            currentChapterIndex = state.currentChapterIndex,
                            currentPageIndex = state.currentPageIndex,
                            adjacentChapters = adjacentChapters,
                            settings = state.readerSettings,
                            scrollRequest = pendingScrollRequest,
                            onToggleMenu = { isChromeVisible = !isChromeVisible },
                            onImageClick = { _, _ -> /* 后续 complete-reader-inline-interactions 实现 */ },
                            onFootnoteClick = { /* 后续 complete-reader-inline-interactions 实现 */ },
                            onLinkClick = { /* 后续 complete-reader-inline-interactions 实现 */ },
                            onParagraphLongClick = { /* 后续 complete-reader-inline-interactions 实现 */ },
                            onScrollPositionChanged = { chapterIndex, anchorId, paragraphIndex, scrollOffset ->
                                viewModel.updateScrollPosition(chapterIndex, anchorId, paragraphIndex, scrollOffset)
                            },
                            onScrollRequestCompleted = { chapterIndex, anchorId, paragraphIndex, scrollOffset ->
                                pendingScrollRequest = null
                                viewModel.onProgrammaticScrollCompleted(
                                    chapterIndex = chapterIndex,
                                    anchorId = anchorId,
                                    paragraphIndex = paragraphIndex,
                                    scrollOffset = scrollOffset
                                )
                            },
                            onCurrentChapterChanged = { newChapterIndex ->
                                viewModel.onScrollChapterChanged(newChapterIndex)
                            },
                            onPagePositionChanged = viewModel::updatePagePosition,
                            onPagerChapterChanged = viewModel::onPagerChapterChanged,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isChromeVisible) {
                            ReaderTopChrome(
                                title = state.bookTitle.ifBlank { fallbackTitle },
                                subtitle = state.currentChapterTitle,
                                isLoadingChapter = shellUiState.showInlineLoading,
                                onBack = { viewModel.back() },
                                onBookDetail = { viewModel.viewBookDetail() },
                                onSettings = { isSettingsVisible = true },
                                modifier = Modifier.align(Alignment.TopCenter)
                            )
                            ReaderBottomMenu(
                                hasPreviousChapter = state.hasPreviousChapter,
                                hasNextChapter = state.hasNextChapter,
                                onPreviousChapter = { viewModel.previousChapter() },
                                onNextChapter = { viewModel.nextChapter() },
                                onTocClick = { isTocVisible = true },
                                onSettingsClick = { isSettingsVisible = true },
                                modifier = Modifier.align(Alignment.BottomCenter)
                            )
                        }
                    }

                    ReaderStatusBar(
                        progressText = footerProgressText,
                        currentTime = currentTime,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            shellUiState.showBlockingError -> {
                ReaderErrorSurface(
                    message = state.error,
                    onRetry = { viewModel.loadBook(bookId, startChapterIndex) },
                    onBack = { viewModel.back() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                ReaderLoadingSurface(modifier = Modifier.fillMaxSize())
            }
        }
    }

    if (isSettingsVisible) {
        ReaderSettingsSheet(
            settings = state.readerSettings,
            onDismiss = { isSettingsVisible = false },
            onFontSizeChange = viewModel::updateFontSize,
            onLineHeightChange = viewModel::updateLineHeight,
            onParagraphSpacingChange = viewModel::updateParagraphSpacing,
            onMarginHorizontalChange = viewModel::updateMarginHorizontal,
            onMarginVerticalChange = viewModel::updateMarginVertical,
            onPageModeChange = viewModel::updatePageMode,
            onFirstLineIndentChange = viewModel::toggleFirstLineIndent,
        )
    }

    if (isTocVisible) {
        ReaderTocSheet(
            tocItems = state.manifest?.toc.orEmpty(),
            currentChapterIndex = state.currentChapterIndex,
            progressPercent = state.progressPercent,
            totalChapters = state.totalChapters,
            bookmarks = state.bookmarks,
            onDismiss = { isTocVisible = false },
            onChapterClick = { chapterIndex ->
                isTocVisible = false
                viewModel.jumpToChapter(chapterIndex)
            },
            onBookmarkClick = { bookmark ->
                isTocVisible = false
                viewModel.jumpToBookmark(bookmark)
            },
            onAddBookmark = { viewModel.addBookmarkAtCurrentPosition() },
            onDeleteBookmark = viewModel::deleteBookmark,
        )
    }

    if (shellUiState.showProgressConflict) {
        ReaderProgressConflictDialog(
            localProgress = state.localProgress,
            remoteProgress = state.remoteProgress,
            onUseLocal = {
                isChromeVisible = true
                viewModel.useLocalProgress()
            },
            onUseRemote = {
                isChromeVisible = true
                viewModel.useRemoteProgress()
            },
            onDismiss = {
                isChromeVisible = true
                viewModel.dismissProgressConflict()
            },
        )
    }
}
