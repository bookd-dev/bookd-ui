package com.bookd.app.screen.reader

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.bookmark_added
import app.composeapp.generated.resources.bookmark_deleted
import app.composeapp.generated.resources.progress_saved
import com.bookd.app.data.vm.ReaderEffect
import com.bookd.app.data.vm.ReaderIntent
import com.bookd.app.data.vm.ReaderViewModel
import com.bookd.app.screen.RouteBookDetail
import com.bookd.app.screen.rememberScreenContext
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
    
    val state by viewModel.state.collectAsState()
    
    // 滚动模式的列表状态
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = state.currentParagraphIndex,
        initialFirstVisibleItemScrollOffset = state.scrollOffset
    )
    
    // 预加载国际化字符串
    val bookmarkAddedMsg = stringResource(Res.string.bookmark_added)
    val bookmarkDeletedMsg = stringResource(Res.string.bookmark_deleted)
    val progressSavedMsg = stringResource(Res.string.progress_saved)
    
    // 初始加载
    LaunchedEffect(bookId) {
        viewModel.onIntent(ReaderIntent.LoadBook(bookId, startChapterIndex))
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
                    // 滚动模式：滚动到指定段落位置
                    screenContext.coroutineScope.launch {
                        listState.scrollToItem(effect.paragraphIndex, effect.offset)
                    }
                }
                is ReaderEffect.ScrollToPage -> {
                    // 翻页模式：页面内部管理，这里不处理
                    // TODO: 如果需要可以通过回调传递
                }
            }
        }
    }
    
    ReaderContent(
        state = state,
        listState = listState,
        onBackClick = { viewModel.onIntent(ReaderIntent.Back) },
        onToggleMenu = { viewModel.onIntent(ReaderIntent.ToggleMenu) },
        onTopMenuClick = { viewModel.onIntent(ReaderIntent.ShowTopMenu) },
        onTopMenuDismiss = { viewModel.onIntent(ReaderIntent.HideTopMenu) },
        onViewBookDetail = { viewModel.onIntent(ReaderIntent.ViewBookDetail) },
        onTocClick = { viewModel.onIntent(ReaderIntent.ShowTocSheet) },
        onTocDismiss = { viewModel.onIntent(ReaderIntent.HideTocSheet) },
        onSettingsClick = { viewModel.onIntent(ReaderIntent.ShowSettingsSheet) },
        onSettingsDismiss = { viewModel.onIntent(ReaderIntent.HideSettingsSheet) },
        onTocItemClick = { index -> viewModel.onIntent(ReaderIntent.JumpToChapter(index)) },
        onTocSortToggle = { viewModel.onIntent(ReaderIntent.ToggleTocSortOrder) },
        onBookmarkClick = { bookmark -> viewModel.onIntent(ReaderIntent.JumpToBookmark(bookmark)) },
        onBookmarkDelete = { bookmarkId -> viewModel.onIntent(ReaderIntent.DeleteBookmark(bookmarkId)) },
        onPreviousChapter = { viewModel.onIntent(ReaderIntent.PreviousChapter) },
        onNextChapter = { viewModel.onIntent(ReaderIntent.NextChapter) },
        onChapterSeek = { index -> viewModel.onIntent(ReaderIntent.JumpToChapter(index)) },
        onPagerChapterChanged = { index -> viewModel.onIntent(ReaderIntent.OnPagerChapterChanged(index)) },
        onScrollPositionChanged = { paragraphIndex, scrollOffset -> 
            viewModel.onIntent(ReaderIntent.UpdateScrollPosition(paragraphIndex, scrollOffset))
        },
        onPagePositionChanged = { pageIndex -> 
            viewModel.onIntent(ReaderIntent.UpdatePagePosition(pageIndex))
        },
        onImageClick = { url, alt -> viewModel.onIntent(ReaderIntent.ShowImagePreview(url, alt)) },
        onImageDismiss = { viewModel.onIntent(ReaderIntent.HideImagePreview) },
        onFootnoteClick = { footnote -> viewModel.onIntent(ReaderIntent.ShowFootnote(footnote)) },
        onFootnoteDismiss = { viewModel.onIntent(ReaderIntent.HideFootnote) },
        onLinkClick = { url ->
            // TODO: 处理链接点击（可能是章节内锚点、外部链接等）
        },
        onParagraphLongClick = { paragraphIndex ->
            viewModel.onIntent(ReaderIntent.ShowBookmarkMenu(paragraphIndex))
        },
        onFontSizeChange = { size -> viewModel.onIntent(ReaderIntent.UpdateFontSize(size)) },
        onLineHeightChange = { height -> viewModel.onIntent(ReaderIntent.UpdateLineHeight(height)) },
        onParagraphSpacingChange = { spacing -> 
            viewModel.onIntent(ReaderIntent.UpdateParagraphSpacing(spacing))
        },
        onMarginHorizontalChange = { margin -> 
            viewModel.onIntent(ReaderIntent.UpdateMarginHorizontal(margin))
        },
        onMarginVerticalChange = { margin -> 
            viewModel.onIntent(ReaderIntent.UpdateMarginVertical(margin))
        },
        onPageModeChange = { mode -> viewModel.onIntent(ReaderIntent.UpdatePageMode(mode)) },
        onPageAnimationTypeChange = { type -> 
            viewModel.onIntent(ReaderIntent.UpdatePageAnimationType(type))
        },
        onFirstLineIndentChange = { _ -> viewModel.onIntent(ReaderIntent.ToggleFirstLineIndent) },
        onProgressConflictUseLocal = { viewModel.onIntent(ReaderIntent.UseLocalProgress) },
        onProgressConflictUseRemote = { viewModel.onIntent(ReaderIntent.UseRemoteProgress) },
        onProgressConflictDismiss = { viewModel.onIntent(ReaderIntent.DismissProgressConflict) }
    )
}
