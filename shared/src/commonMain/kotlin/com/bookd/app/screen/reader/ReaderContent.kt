package com.bookd.app.screen.reader

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.screen.reader.content.ScrollModeContent


@Composable
fun ReaderContent(
    bookId: Int,
    currentChapterIndex: Int,
    adjacentChapters: Map<Int, ChapterContent>,
    settings: ReaderSettings,
    onToggleMenu: () -> Unit,
    onImageClick: (url: String, alt: String?) -> Unit,
    onFootnoteClick: (ContentElement.Footnote) -> Unit,
    onLinkClick: (url: String) -> Unit,
    onParagraphLongClick: (paragraphIndex: Int) -> Unit,
    onScrollPositionChanged: (chapterIndex: Int, paragraphIndex: Int, scrollOffset: Int) -> Unit,
    onCurrentChapterChanged: (chapterIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // 目前仅实现滚动模式；翻页模式后续在此处按 settings.pageMode 分支
    ScrollModeContent(
        bookId = bookId,
        currentChapterIndex = currentChapterIndex,
        adjacentChapters = adjacentChapters,
        settings = settings,
        onToggleMenu = onToggleMenu,
        onImageClick = onImageClick,
        onFootnoteClick = onFootnoteClick,
        onLinkClick = onLinkClick,
        onParagraphLongClick = onParagraphLongClick,
        onScrollPositionChanged = onScrollPositionChanged,
        onCurrentChapterChanged = onCurrentChapterChanged,
        modifier = modifier.fillMaxSize()
    )
}

