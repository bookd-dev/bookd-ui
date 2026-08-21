package com.bookd.app.screen.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.PageMode
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.screen.reader.content.PageModeContent
import com.bookd.app.screen.reader.content.ReaderScrollRequest
import com.bookd.app.screen.reader.content.ScrollModeContent


@Composable
fun ReaderContent(
    bookId: Int,
    currentChapterIndex: Int,
    currentPageIndex: Int,
    adjacentChapters: Map<Int, ChapterContent>,
    settings: ReaderSettings,
    scrollRequest: ReaderScrollRequest?,
    onToggleMenu: () -> Unit,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onParagraphLongClick: (ReaderParagraphSelection) -> Unit,
    onScrollPositionChanged: (chapterIndex: Int, anchorId: String?, paragraphIndex: Int, scrollOffset: Int) -> Unit,
    onScrollRequestCompleted: (
        sequence: Long,
        chapterIndex: Int,
        pageIndex: Int?,
        anchorId: String?,
        paragraphIndex: Int,
        scrollOffset: Int,
    ) -> Unit,
    onCurrentChapterChanged: (chapterIndex: Int) -> Unit,
    onPagePositionChanged: (chapterIndex: Int, pageIndex: Int, anchorId: String?, paragraphIndex: Int) -> Unit,
    onPagerChapterChanged: (chapterIndex: Int, direction: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when (settings.pageMode) {
        PageMode.SCROLL -> {
            ScrollModeContent(
                bookId = bookId,
                currentChapterIndex = currentChapterIndex,
                adjacentChapters = adjacentChapters,
                settings = settings,
                scrollRequest = scrollRequest,
                onToggleMenu = onToggleMenu,
                onImageClick = onImageClick,
                onFootnoteClick = onFootnoteClick,
                onLinkClick = onLinkClick,
                onParagraphLongClick = onParagraphLongClick,
                onScrollPositionChanged = onScrollPositionChanged,
                onScrollRequestCompleted = onScrollRequestCompleted,
                onCurrentChapterChanged = onCurrentChapterChanged,
                modifier = modifier.fillMaxSize()
            )
        }
        PageMode.PAGE -> {
            PageModeContent(
                bookId = bookId,
                currentChapterIndex = currentChapterIndex,
                currentPageIndex = currentPageIndex,
                adjacentChapters = adjacentChapters,
                settings = settings,
                scrollRequest = scrollRequest,
                onToggleMenu = onToggleMenu,
                onImageClick = onImageClick,
                onFootnoteClick = onFootnoteClick,
                onLinkClick = onLinkClick,
                onParagraphLongClick = onParagraphLongClick,
                onScrollRequestCompleted = onScrollRequestCompleted,
                onPagePositionChanged = onPagePositionChanged,
                onPagerChapterChanged = onPagerChapterChanged,
                modifier = modifier.fillMaxSize()
            )
        }
    }
}
