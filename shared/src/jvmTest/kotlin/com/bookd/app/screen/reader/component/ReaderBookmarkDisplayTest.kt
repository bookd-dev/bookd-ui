package com.bookd.app.screen.reader.component

import com.bookd.app.data.model.BookmarkResponse
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.TextSpan
import com.bookd.app.data.model.TocItem
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderBookmarkDisplayTest {

    @Test
    fun `given paragraph bookmark when display item built then uses chapter title and paragraph text`() {
        val item = buildReaderBookmarkDisplayItem(
            bookmark = bookmark(anchorId = "p-2", paragraphIndex = 2),
            tocItems = listOf(TocItem(index = 1, title = "第一章")),
            chapterContents = mapOf(
                1 to ChapterContent(
                    index = 1,
                    title = "第一章",
                    elements = listOf(
                        ContentElement.Paragraph(listOf(TextSpan("第一段")), anchorId = "p-1"),
                        ContentElement.Paragraph(listOf(TextSpan("第二段内容很长")), anchorId = "p-2"),
                    ),
                    prevIndex = 0,
                    nextIndex = 2,
                )
            ),
        )

        assertEquals("第一章", item.chapterTitle)
        assertEquals("第二段内容很长", item.paragraphText)
        assertTrue(item.isParagraphBookmark)
    }

    @Test
    fun `given chapter bookmark when display item built then only marks chapter context`() {
        val item = buildReaderBookmarkDisplayItem(
            bookmark = bookmark(anchorId = null, paragraphIndex = null),
            tocItems = listOf(TocItem(index = 1, title = "第一章")),
            chapterContents = emptyMap(),
        )

        assertEquals("第一章", item.chapterTitle)
        assertEquals(null, item.paragraphText)
        assertFalse(item.isParagraphBookmark)
    }

    @Test
    fun `given bookmark timestamp when formatted then keeps compact month day and minute`() {
        assertEquals("06-04 11:02", formatReaderBookmarkTimestamp("2026-06-04T11:02:26.141"))
        assertEquals("06-04 11:02", formatReaderBookmarkTimestamp("2026-06-04 11:02:26"))
        assertEquals("bad-time", formatReaderBookmarkTimestamp("bad-time"))
    }

    private fun bookmark(anchorId: String?, paragraphIndex: Int?): BookmarkResponse {
        return BookmarkResponse(
            id = 1,
            bookId = 9,
            chapterIndex = 1,
            anchorId = anchorId,
            paragraphIndex = paragraphIndex,
            createdAt = "2026-06-04T00:00:00Z",
        )
    }
}
