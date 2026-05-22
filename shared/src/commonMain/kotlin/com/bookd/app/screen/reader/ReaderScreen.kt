package com.bookd.app.screen.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.bookd.app.basic.extension.logD
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.bookmark_added
import app.composeapp.generated.resources.bookmark_deleted
import app.composeapp.generated.resources.progress_saved
import com.bookd.app.data.vm.ReaderEffect
import com.bookd.app.data.vm.ReaderViewModel
import com.bookd.app.screen.RouteBookDetail
import com.bookd.app.screen.rememberScreenContext
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

    // 预加载国际化字符串
    val bookmarkAddedMsg = stringResource(Res.string.bookmark_added)
    val bookmarkDeletedMsg = stringResource(Res.string.bookmark_deleted)
    val progressSavedMsg = stringResource(Res.string.progress_saved)

    // 初始加载
    LaunchedEffect(bookId) {
        viewModel.loadBook(bookId, startChapterIndex)
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
                    // TODO: 书签/目录跳转时，通过 listState.scrollToItem 处理
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

    if (state.hasProgressConflict) {
        viewModel.useLocalProgress()
    }

    if (!state.isLoading && currentChapter != null) {
        ReaderContent(
            bookId = state.bookId,
            currentChapterIndex = state.currentChapterIndex,
            adjacentChapters = adjacentChapters,
            settings = state.readerSettings,
            onToggleMenu = { /* TODO: 菜单覆盖层 */ },
            onImageClick = { _, _ -> /* TODO: 图片预览 */ },
            onFootnoteClick = { /* TODO: 脚注弹窗 */ },
            onLinkClick = { /* TODO: 外部链接处理 */ },
            onParagraphLongClick = { /* TODO: 长按段落 */ },
            onScrollPositionChanged = { chapterIndex, paragraphIndex, scrollOffset ->
                viewModel.updateScrollPosition(chapterIndex, paragraphIndex, scrollOffset)
            },
            onCurrentChapterChanged = { newChapterIndex ->
                viewModel.onScrollChapterChanged(newChapterIndex)
            }
        )
    }
}
