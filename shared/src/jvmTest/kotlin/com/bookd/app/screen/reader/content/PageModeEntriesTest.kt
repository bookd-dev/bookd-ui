package com.bookd.app.screen.reader.content

import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.TextSpan
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class PageModeEntriesTest {

    @Test
    fun `given loaded chapter anchors when build page mode entries then creates stable content pages`() {
        val elements = listOf(paragraph("first"), paragraph("second"))
        val anchors = listOf(
            PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0),
            PageAnchor(elementIndex = 1, textOffset = 0, pageIndex = 1)
        )

        val entries = buildPageModeEntries(
            orderedChapterIndices = listOf(3),
            chapterElements = mapOf(3 to elements),
            chapterAnchors = mapOf(3 to anchors)
        )

        assertEquals(2, entries.size)
        val first = assertIs<ContentPageEntry>(entries[0])
        val second = assertIs<ContentPageEntry>(entries[1])
        assertEquals("page:3:0", first.key)
        assertEquals("page:3:1", second.key)
        assertEquals(anchors[0], first.anchor)
        assertEquals(anchors[1], first.nextAnchor)
        assertNull(second.nextAnchor)
    }

    @Test
    fun `given chapter without anchors when build page mode entries then creates loading placeholder`() {
        val entries = buildPageModeEntries(
            orderedChapterIndices = listOf(2),
            chapterElements = mapOf(2 to listOf(paragraph("pending"))),
            chapterAnchors = emptyMap()
        )

        val loading = assertIs<LoadingPageEntry>(entries.single())
        assertEquals(2, loading.chapterIndex)
        assertEquals(0, loading.pageIndex)
        assertEquals("loading:2", loading.key)
    }

    @Test
    fun `given title only chapter when build chapter elements then keeps chapter reachable`() {
        val chapter = ChapterContent(
            index = 8,
            title = "封面",
            elements = emptyList(),
            prevIndex = 7,
            nextIndex = 9
        )

        val elements = buildReaderChapterElements(chapter)
        val entries = buildPageModeEntries(
            orderedChapterIndices = listOf(8),
            chapterElements = mapOf(8 to elements),
            chapterAnchors = mapOf(8 to listOf(PageAnchor(0, 0, 0)))
        )

        assertEquals(listOf(ContentElement.Heading(level = 1, text = "封面")), elements)
        val page = assertIs<ContentPageEntry>(entries.single())
        assertEquals(8, page.chapterIndex)
        assertEquals(0, page.pageIndex)
    }

    @Test
    fun `given entries across chapters when find entry and direction then handles chapter boundaries`() {
        val entries = buildPageModeEntries(
            orderedChapterIndices = listOf(4, 5),
            chapterElements = mapOf(
                4 to listOf(paragraph("previous")),
                5 to listOf(paragraph("next"))
            ),
            chapterAnchors = mapOf(
                4 to listOf(PageAnchor(0, 0, 0), PageAnchor(0, 20, 1)),
                5 to listOf(PageAnchor(0, 0, 0))
            )
        )

        assertEquals(1, findPageModeEntryIndex(entries, chapterIndex = 4, pageIndex = 1))
        assertEquals(2, findFirstChapterEntryIndex(entries, chapterIndex = 5))
        assertEquals(1, pageModeChapterDirection(fromChapterIndex = 4, toChapterIndex = 5))
        assertEquals(-1, pageModeChapterDirection(fromChapterIndex = 5, toChapterIndex = 4))
        assertEquals(0, pageModeChapterDirection(fromChapterIndex = 5, toChapterIndex = 5))
    }

    @Test
    fun `given bookmark anchor when resolve page mode scroll target then returns containing page`() {
        val elements = listOf(
            ContentElement.Paragraph(spans = listOf(TextSpan("first")), anchorId = "p-0"),
            ContentElement.Paragraph(spans = listOf(TextSpan("second")), anchorId = "p-1"),
            ContentElement.Paragraph(spans = listOf(TextSpan("third")), anchorId = "p-2"),
        )
        val anchors = listOf(
            PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0),
            PageAnchor(elementIndex = 2, textOffset = 0, pageIndex = 1),
        )
        val entries = buildPageModeEntries(
            orderedChapterIndices = listOf(6),
            chapterElements = mapOf(6 to elements),
            chapterAnchors = mapOf(6 to anchors),
        )

        val target = resolvePageModeScrollTarget(
            entries = entries,
            chapterElements = mapOf(6 to elements),
            chapterAnchors = mapOf(6 to anchors),
            request = ReaderScrollRequest(
                sequence = 1L,
                chapterIndex = 6,
                anchorId = "p-2",
                paragraphIndex = 0,
                offset = 0,
            ),
        )

        requireNotNull(target)
        assertEquals(1, target.pagerIndex)
        assertEquals(1, target.pageIndex)
        assertEquals("p-2", target.anchorId)
        assertEquals(2, target.paragraphIndex)
    }

    @Test
    fun `given bookmark anchor inside page when resolve page mode scroll target then preserves target paragraph`() {
        val elements = listOf(
            ContentElement.Paragraph(spans = listOf(TextSpan("first")), anchorId = "p-0"),
            ContentElement.Paragraph(spans = listOf(TextSpan("second")), anchorId = "p-1"),
            ContentElement.Paragraph(spans = listOf(TextSpan("third")), anchorId = "p-2"),
        )
        val anchors = listOf(
            PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0),
            PageAnchor(elementIndex = 2, textOffset = 0, pageIndex = 1),
        )
        val entries = buildPageModeEntries(
            orderedChapterIndices = listOf(6),
            chapterElements = mapOf(6 to elements),
            chapterAnchors = mapOf(6 to anchors),
        )

        val target = resolvePageModeScrollTarget(
            entries = entries,
            chapterElements = mapOf(6 to elements),
            chapterAnchors = mapOf(6 to anchors),
            request = ReaderScrollRequest(
                sequence = 1L,
                chapterIndex = 6,
                anchorId = "p-1",
                paragraphIndex = 0,
                offset = 0,
            ),
        )

        requireNotNull(target)
        assertEquals(0, target.pagerIndex)
        assertEquals(0, target.pageIndex)
        assertEquals("p-1", target.anchorId)
        assertEquals(1, target.paragraphIndex)
    }

    @Test
    fun `given missing bookmark anchor when resolve page mode scroll target then uses fallback page`() {
        val elements = listOf(
            ContentElement.Paragraph(spans = listOf(TextSpan("first")), anchorId = "p-0"),
            ContentElement.Paragraph(spans = listOf(TextSpan("second")), anchorId = "p-1"),
        )
        val anchors = listOf(
            PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0),
            PageAnchor(elementIndex = 1, textOffset = 0, pageIndex = 1),
        )
        val entries = buildPageModeEntries(
            orderedChapterIndices = listOf(2),
            chapterElements = mapOf(2 to elements),
            chapterAnchors = mapOf(2 to anchors),
        )

        val target = resolvePageModeScrollTarget(
            entries = entries,
            chapterElements = mapOf(2 to elements),
            chapterAnchors = mapOf(2 to anchors),
            request = ReaderScrollRequest(
                sequence = 1L,
                chapterIndex = 2,
                anchorId = "missing",
                paragraphIndex = 9,
                offset = 0,
            ),
        )

        requireNotNull(target)
        assertEquals(1, target.pagerIndex)
        assertEquals(1, target.pageIndex)
        assertEquals("p-1", target.anchorId)
    }

    private fun paragraph(text: String): ContentElement {
        return ContentElement.Paragraph(spans = listOf(TextSpan(text)))
    }
}
