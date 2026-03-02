package com.bookd.app.screen.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bookd.app.data.model.BookmarkResponse
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.PageMode
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.vm.ReaderState
import com.bookd.app.screen.reader.component.ReaderFootnoteDialog
import com.bookd.app.screen.reader.component.ReaderImagePreview
import com.bookd.app.screen.reader.component.ReaderMenuBar
import com.bookd.app.screen.reader.component.ReaderProgressConflictDialog
import com.bookd.app.screen.reader.component.ReaderSettingsSheet
import com.bookd.app.screen.reader.component.ReaderStatusBar
import com.bookd.app.screen.reader.component.ReaderTocSheet
import com.bookd.app.screen.reader.component.ReaderTopBar
import com.bookd.app.screen.reader.content.PageModeContent
import com.bookd.app.screen.reader.content.ScrollModeContent
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.loading
import app.composeapp.generated.resources.load_failed
import org.jetbrains.compose.resources.stringResource

/**
 * 阅读器主内容组件
 * 
 * 纯 UI 组件，可预览
 */
@Composable
fun ReaderContent(
    state: ReaderState,
    listState: LazyListState,
    onBackClick: () -> Unit,
    onToggleMenu: () -> Unit,
    onTopMenuClick: () -> Unit,
    onTopMenuDismiss: () -> Unit,
    onViewBookDetail: () -> Unit,
    onTocClick: () -> Unit,
    onTocDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onSettingsDismiss: () -> Unit,
    onTocItemClick: (Int) -> Unit,
    onTocSortToggle: () -> Unit,
    onBookmarkClick: (BookmarkResponse) -> Unit,
    onBookmarkDelete: (Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit,
    onChapterSeek: (Int) -> Unit,
    onPagerChapterChanged: (Int, Int) -> Unit,
    onScrollPositionChanged: (Int, Int) -> Unit,
    onPagePositionChanged: (Int) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onImageDismiss: () -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onFootnoteDismiss: () -> Unit,
    onLinkClick: (String) -> Unit,
    onParagraphLongClick: (Int) -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onLineHeightChange: (Double) -> Unit,
    onParagraphSpacingChange: (Int) -> Unit,
    onMarginHorizontalChange: (Int) -> Unit,
    onMarginVerticalChange: (Int) -> Unit,
    onPageModeChange: (PageMode) -> Unit,
    onPageAnimationTypeChange: (com.bookd.app.data.model.PageAnimationType) -> Unit,
    onFirstLineIndentChange: (Boolean) -> Unit,
    onProgressConflictUseLocal: () -> Unit,
    onProgressConflictUseRemote: () -> Unit,
    onProgressConflictDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {

    Scaffold(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .consumeWindowInsets(WindowInsets.statusBars)
            .consumeWindowInsets(WindowInsets.navigationBars),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 主内容区域
            when {
                state.isLoading -> {
                    // 加载中
                    LoadingContent()
                }
                state.error != null -> {
                    // 错误
                    ErrorContent(error = state.error)
                }
                state.currentChapter != null -> {
                    // 阅读内容
                    ReadingContent(
                        bookId = state.bookId,
                        chapter = state.currentChapter,
                        adjacentChapters = state.adjacentChapters,
                        currentChapterIndex = state.currentChapterIndex,
                        pagerSlideDirection = state.pagerSlideDirection,
                        settings = state.readerSettings,
                        showMenu = state.showMenu,
                        listState = listState,
                        onToggleMenu = onToggleMenu,
                        onScrollPositionChanged = onScrollPositionChanged,
                        onPagePositionChanged = onPagePositionChanged,
                        onImageClick = onImageClick,
                        onFootnoteClick = onFootnoteClick,
                        onLinkClick = onLinkClick,
                        onParagraphLongClick = onParagraphLongClick,
                        onChapterChanged = onPagerChapterChanged,
                        onPreviousChapter = onPreviousChapter,
                        onNextChapter = onNextChapter
                    )
                }
            }

            // 顶部栏（菜单显示时）
            AnimatedVisibility(
                visible = state.showMenu,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                ReaderTopBar(
                    chapterTitle = state.currentChapterTitle ?: stringResource(Res.string.loading),
                    currentChapter = state.currentChapterIndex,
                    totalChapters = state.totalChapters,
                    showMenu = state.showTopMenu,
                    onBackClick = onBackClick,
                    onMenuClick = onTopMenuClick,
                    onMenuDismiss = onTopMenuDismiss,
                    onViewBookDetail = onViewBookDetail
                )
            }

            // 底部菜单栏（菜单显示时）
            AnimatedVisibility(
                visible = state.showMenu,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderMenuBar(
                    currentChapter = state.currentChapterIndex,
                    totalChapters = state.totalChapters,
                    hasPreviousChapter = state.hasPreviousChapter,
                    hasNextChapter = state.hasNextChapter,
                    onPreviousChapter = onPreviousChapter,
                    onNextChapter = onNextChapter,
                    onChapterSeek = onChapterSeek,
                    onTocClick = onTocClick,
                    onSettingsClick = onSettingsClick
                )
            }

            // 底部状态栏（菜单隐藏时）
            AnimatedVisibility(
                visible = !state.showMenu && state.currentChapter != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ReaderStatusBar(
                    currentPage = state.currentParagraphIndex,
                    totalPages = state.currentChapter?.elements?.size ?: 0
                )
            }

            // 目录/书签面板
            if (state.showTocSheet) {
                ReaderTocSheet(
                    toc = state.sortedToc,
                    bookmarks = state.bookmarks,
                    currentChapterIndex = state.currentChapterIndex,
                    sortOrder = state.tocSortOrder,
                    onDismiss = onTocDismiss,
                    onTocItemClick = onTocItemClick,
                    onBookmarkClick = onBookmarkClick,
                    onBookmarkDelete = onBookmarkDelete,
                    onSortOrderToggle = onTocSortToggle
                )
            }

            // 设置面板
            if (state.showSettingsSheet) {
                ReaderSettingsSheet(
                    settings = state.readerSettings,
                    onDismiss = onSettingsDismiss,
                    onFontSizeChange = onFontSizeChange,
                    onLineHeightChange = onLineHeightChange,
                    onParagraphSpacingChange = onParagraphSpacingChange,
                    onMarginHorizontalChange = onMarginHorizontalChange,
                    onMarginVerticalChange = onMarginVerticalChange,
                    onPageModeChange = onPageModeChange,
                    onPageAnimationTypeChange = onPageAnimationTypeChange,
                    onFirstLineIndentChange = onFirstLineIndentChange
                )
            }

            // 图片预览
            if (state.showImagePreview && state.previewImageUrl != null) {
                ReaderImagePreview(
                    imageUrl = state.previewImageUrl,
                    imageAlt = state.previewImageAlt,
                    onDismiss = onImageDismiss
                )
            }

            // 脚注弹窗
            if (state.showFootnoteDialog && state.currentFootnote != null) {
                ReaderFootnoteDialog(
                    footnote = state.currentFootnote,
                    onDismiss = onFootnoteDismiss
                )
            }

            // 进度冲突对话框
            if (state.showProgressConflictDialog) {
                ReaderProgressConflictDialog(
                    localProgress = state.localProgress,
                    remoteProgress = state.remoteProgress,
                    onUseLocal = onProgressConflictUseLocal,
                    onUseRemote = onProgressConflictUseRemote,
                    onDismiss = onProgressConflictDismiss
                )
            }
        }
    }
}

/**
 * 阅读内容区域
 */
@Composable
private fun ReadingContent(
    bookId: Int,
    chapter: ChapterContent,
    adjacentChapters: Map<Int, ChapterContent>,
    currentChapterIndex: Int,
    pagerSlideDirection: Int,
    settings: ReaderSettings,
    showMenu: Boolean,
    listState: LazyListState,
    onToggleMenu: () -> Unit,
    onScrollPositionChanged: (Int, Int) -> Unit,
    onPagePositionChanged: (Int) -> Unit,
    onImageClick: (String, String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (String) -> Unit,
    onParagraphLongClick: (Int) -> Unit,
    onChapterChanged: (Int, Int) -> Unit,
    onPreviousChapter: () -> Unit,
    onNextChapter: () -> Unit
) {
    // 阅读区域 - 不再在外层 Box 添加点击事件，而是在具体的内容组件中处理
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (settings.pageMode) {
            PageMode.SCROLL -> {
                ScrollModeContent(
                    bookId = bookId,
                    chapterIndex = currentChapterIndex,
                    chapter = chapter,
                    settings = settings,
                    listState = listState,
                    onToggleMenu = onToggleMenu,
                    onImageClick = onImageClick,
                    onFootnoteClick = onFootnoteClick,
                    onLinkClick = onLinkClick,
                    onParagraphLongClick = onParagraphLongClick,
                    onScrollPositionChanged = onScrollPositionChanged,
                    onReachEnd = onNextChapter,
                    onReachStart = { /* 已经在第一章时不做处理 */ }
                )
            }
            
            PageMode.PAGE -> {
                PageModeContent(
                    bookId = bookId,
                    chapters = adjacentChapters,
                    currentChapterIndex = currentChapterIndex,
                    pagerSlideDirection = pagerSlideDirection,
                    settings = settings,
                    onToggleMenu = onToggleMenu,
                    onImageClick = onImageClick,
                    onFootnoteClick = onFootnoteClick,
                    onLinkClick = onLinkClick,
                    onParagraphLongClick = onParagraphLongClick,
                    onPageChanged = onPagePositionChanged,
                    onChapterChanged = onChapterChanged
                )
            }
        }
    }
}

/**
 * 加载中内容
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 错误内容
 */
@Composable
private fun ErrorContent(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(Res.string.load_failed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
