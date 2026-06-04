package com.bookd.app.screen.bookshelf.component

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookshelfBookItemLayoutTest {

    @Test
    fun `given list layout metrics when book has sparse metadata then bottom row can stay cover anchored`() {
        assertTrue(BookshelfBookItemLayout.ListCoverHeight > BookshelfBookItemLayout.ListBottomRowHeight)
        assertEquals(
            BookshelfBookItemLayout.ContextMenuTouchTarget,
            BookshelfBookItemLayout.ListBottomRowHeight,
        )
        assertEquals(
            BookshelfBookItemLayout.ListBottomRowHeight + BookshelfBookItemLayout.BottomRowContentGap,
            BookshelfBookItemLayout.ListTextBottomPadding,
        )
    }

    @Test
    fun `given grid layout metrics when title or author length changes then controls keep a stable slot`() {
        assertEquals(BookshelfBookItemLayout.GridCoverWidth, BookshelfBookItemLayout.GridInfoWidth)
        assertTrue(BookshelfBookItemLayout.GridInfoHeight > BookshelfBookItemLayout.GridBottomRowHeight)
        assertEquals(1, BookshelfBookItemLayout.GridTitleMinLines)
        assertEquals(2, BookshelfBookItemLayout.GridTitleMaxLines)
        assertEquals(
            BookshelfBookItemLayout.ContextMenuTouchTarget,
            BookshelfBookItemLayout.GridBottomRowHeight,
        )
        assertEquals(
            BookshelfBookItemLayout.GridBottomRowHeight + BookshelfBookItemLayout.BottomRowContentGap,
            BookshelfBookItemLayout.GridTextBottomPadding,
        )
    }
}
