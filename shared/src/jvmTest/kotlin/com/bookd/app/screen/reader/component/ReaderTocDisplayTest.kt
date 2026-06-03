package com.bookd.app.screen.reader.component

import com.bookd.app.data.model.TocItem
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReaderTocDisplayTest {

    @Test
    fun `given nested toc when build display items then flattens in reading order`() {
        val items = buildReaderTocDisplayItems(
            tocItems = tocTree(),
            currentChapterIndex = 2,
            currentProgressPercent = 18,
            descending = false,
        )

        assertEquals(listOf(0, 1, 2, 3), items.map { it.item.index })
        assertTrue(items.single { it.item.index == 2 }.selected)
        assertFalse(items.single { it.item.index == 1 }.selected)
    }

    @Test
    fun `given descending sort when build display items then reverses flattened order`() {
        val items = buildReaderTocDisplayItems(
            tocItems = tocTree(),
            currentChapterIndex = 2,
            currentProgressPercent = 18,
            descending = true,
        )

        assertEquals(listOf(3, 2, 1, 0), items.map { it.item.index })
    }

    @Test
    fun `given current chapter progress when build display items then uses live progress for selected item`() {
        val items = buildReaderTocDisplayItems(
            tocItems = tocTree(),
            currentChapterIndex = 2,
            currentProgressPercent = 18,
            descending = false,
        )

        assertEquals(18, items.single { it.item.index == 2 }.readPercent)
        assertEquals(45, items.single { it.item.index == 3 }.readPercent)
    }

    @Test
    fun `given display items when find current index then returns selected row index`() {
        val items = buildReaderTocDisplayItems(
            tocItems = tocTree(),
            currentChapterIndex = 2,
            currentProgressPercent = 18,
            descending = true,
        )

        assertEquals(1, findReaderTocCurrentIndex(items))
    }

    @Test
    fun `given display items without selected chapter when find current index then returns minus one`() {
        val items = buildReaderTocDisplayItems(
            tocItems = tocTree(),
            currentChapterIndex = 99,
            currentProgressPercent = 18,
            descending = false,
        )

        assertEquals(-1, findReaderTocCurrentIndex(items))
    }

    @Test
    fun `given toc item with image count when build display items then keeps image metadata`() {
        val items = buildReaderTocDisplayItems(
            tocItems = tocTree(),
            currentChapterIndex = 2,
            currentProgressPercent = 18,
            descending = false,
        )

        assertEquals(3, items.single { it.item.index == 2 }.item.imageCount)
    }

    private fun tocTree(): List<TocItem> {
        return listOf(
            TocItem(
                index = 0,
                title = "第一卷",
                level = 0,
                children = listOf(
                    TocItem(index = 1, title = "第1章", level = 1, wordCount = 2180),
                    TocItem(index = 2, title = "第2章", level = 1, wordCount = 2158, imageCount = 3, readProgress = 0.01),
                ),
            ),
            TocItem(index = 3, title = "第3章", level = 0, wordCount = 2297, readProgress = 0.45),
        )
    }
}
