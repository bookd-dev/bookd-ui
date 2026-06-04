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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.bookd.app.basic.extension.logD
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.app_name
import app.composeapp.generated.resources.bookmark_added
import app.composeapp.generated.resources.bookmark_deleted
import app.composeapp.generated.resources.progress_saved
import app.composeapp.generated.resources.reader_internal_link_unavailable
import app.composeapp.generated.resources.reader_loading_chapter
import com.bookd.app.basic.extension.getCurrentTimeString
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.vm.ReaderEffect
import com.bookd.app.data.vm.ReaderViewModel
import com.bookd.app.screen.RouteBookDetail
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.screen.reader.component.ReaderBottomMenu
import com.bookd.app.screen.reader.component.ReaderErrorSurface
import com.bookd.app.screen.reader.component.ReaderExternalLinkFallbackSheet
import com.bookd.app.screen.reader.component.ReaderFootnoteSheet
import com.bookd.app.screen.reader.component.ReaderImagePreviewDialog
import com.bookd.app.screen.reader.component.ReaderLoadingSurface
import com.bookd.app.screen.reader.component.ReaderParagraphActionsSheet
import com.bookd.app.screen.reader.component.ReaderProgressConflictDialog
import com.bookd.app.screen.reader.component.ReaderSettingsSheet
import com.bookd.app.screen.reader.component.ReaderStatusBar
import com.bookd.app.screen.reader.component.ReaderTocSheet
import com.bookd.app.screen.reader.component.ReaderTopChrome
import com.bookd.app.screen.reader.content.ReaderScrollRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
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
    val coroutineScope = screenContext.coroutineScope
    val uriHandler = LocalUriHandler.current

    val state by viewModel.state.collectAsState()
    var isChromeVisible by remember { mutableStateOf(true) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var isTocVisible by remember { mutableStateOf(false) }
    var currentTime by remember { mutableStateOf(getCurrentTimeString()) }
    var pendingScrollRequest by remember { mutableStateOf<ReaderScrollRequest?>(null) }
    var imagePreview by remember { mutableStateOf<ReaderImagePreview?>(null) }
    var footnotePreview by remember { mutableStateOf<ContentElement.Footnote?>(null) }
    var externalLinkFallbackUrl by remember { mutableStateOf<String?>(null) }
    var paragraphSelection by remember { mutableStateOf<ReaderParagraphSelection?>(null) }
    val shellUiState = buildReaderShellUiState(state)

    // 预加载国际化字符串
    val fallbackTitle = stringResource(Res.string.app_name)
    val bookmarkAddedMsg = stringResource(Res.string.bookmark_added)
    val bookmarkDeletedMsg = stringResource(Res.string.bookmark_deleted)
    val progressSavedMsg = stringResource(Res.string.progress_saved)
    val internalLinkUnavailableMsg = stringResource(Res.string.reader_internal_link_unavailable)
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
                }
                is ReaderEffect.ScrollToPage -> {
                    // 翻页模式跳页（翻页模式实现时处理）
                }
            }
        }
    }

    val currentChapter = state.currentChapter
    val adjacentChapters = state.adjacentChapters
    fun openExternalLink(url: String): ReaderExternalLinkResult {
        return openReaderExternalLink(url) { uriHandler.openUri(it) }
    }

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
                            onImageClick = { url, alt ->
                                imagePreview = ReaderImagePreview(url, alt)
                                isChromeVisible = false
                            },
                            onFootnoteClick = { footnote ->
                                footnotePreview = footnote
                                isChromeVisible = false
                            },
                            onLinkClick = { url ->
                                val manifest = state.manifest
                                val internalTarget = manifest?.let {
                                    resolveReaderInternalLink(
                                        url = url,
                                        manifest = it,
                                        currentChapterIndex = state.currentChapterIndex,
                                    )
                                }
                                when {
                                    internalTarget != null -> {
                                        viewModel.jumpToInternalLink(
                                            chapterIndex = internalTarget.chapterIndex,
                                            anchorId = internalTarget.anchorId,
                                        )
                                        isChromeVisible = false
                                    }
                                    isReaderInternalDocumentLink(url) -> {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar(internalLinkUnavailableMsg)
                                        }
                                    }
                                    else -> {
                                        when (val result = openExternalLink(url)) {
                                            ReaderExternalLinkResult.Opened -> Unit
                                            is ReaderExternalLinkResult.Fallback -> {
                                                externalLinkFallbackUrl = result.url
                                                isChromeVisible = false
                                            }
                                        }
                                    }
                                }
                            },
                            onParagraphLongClick = { selection ->
                                paragraphSelection = selection
                                isChromeVisible = false
                            },
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
        val bookmarkChapterContents = state.preloadedChapters +
            state.adjacentChapters +
            listOfNotNull(state.currentChapter).associateBy { it.index }
        ReaderTocSheet(
            tocItems = state.manifest?.toc.orEmpty(),
            currentChapterIndex = state.currentChapterIndex,
            progressPercent = state.progressPercent,
            totalChapters = state.totalChapters,
            bookmarks = state.bookmarks,
            chapterContents = bookmarkChapterContents,
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

    imagePreview?.let { preview ->
        ReaderImagePreviewDialog(
            preview = preview,
            onDismiss = { imagePreview = null },
        )
    }

    footnotePreview?.let { footnote ->
        ReaderFootnoteSheet(
            footnote = footnote,
            onDismiss = { footnotePreview = null },
        )
    }

    externalLinkFallbackUrl?.let { url ->
        ReaderExternalLinkFallbackSheet(
            url = url,
            onDismiss = { externalLinkFallbackUrl = null },
            onOpen = {
                when (val result = openExternalLink(url)) {
                    ReaderExternalLinkResult.Opened -> externalLinkFallbackUrl = null
                    is ReaderExternalLinkResult.Fallback -> externalLinkFallbackUrl = result.url
                }
            },
        )
    }

    paragraphSelection?.let { selection ->
        ReaderParagraphActionsSheet(
            selection = selection,
            onDismiss = { paragraphSelection = null },
            onAddBookmark = {
                val request = buildReaderParagraphBookmarkRequest(selection)
                viewModel.addBookmark(
                    chapterIndex = request.chapterIndex,
                    anchorId = request.anchorId,
                    paragraphIndex = request.paragraphIndex,
                    scrollOffset = request.scrollOffset,
                    note = request.note,
                )
                paragraphSelection = null
            },
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
