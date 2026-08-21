package com.bookd.app.screen.reader.content

import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.TextSpan
import org.junit.Test
import kotlin.test.assertEquals

class ReaderScrollAnchorResolutionTest {

    @Test
    fun `given matching anchor when resolve then uses stable content anchor before fallback`() {
        val result = resolveReaderScrollAnchor(
            elements = elements(),
            pageAnchors = pageAnchors(),
            anchorId = "p3",
            fallbackIndex = 0,
        )

        assertEquals(2, result.anchorIndex)
        assertEquals("p3", result.anchorId)
        assertEquals(3, result.targetElementIndex)
    }

    @Test
    fun `given target element inside a page when resolve then returns containing page anchor`() {
        val result = resolveReaderScrollAnchor(
            elements = elements(),
            pageAnchors = listOf(
                PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0),
                PageAnchor(elementIndex = 3, textOffset = 0, pageIndex = 1),
            ),
            anchorId = "p2",
            fallbackIndex = 0,
        )

        assertEquals(0, result.anchorIndex)
        assertEquals("p2", result.anchorId)
        assertEquals(2, result.targetElementIndex)
    }

    @Test
    fun `given missing anchor when resolve then clamps fallback index to available page anchors`() {
        val result = resolveReaderScrollAnchor(
            elements = elements(),
            pageAnchors = pageAnchors(),
            anchorId = "missing",
            fallbackIndex = 99,
        )

        assertEquals(2, result.anchorIndex)
        assertEquals("p3", result.anchorId)
        assertEquals(3, result.targetElementIndex)
    }

    @Test
    fun `given no page anchors when resolve then returns safe zero resolution`() {
        val result = resolveReaderScrollAnchor(
            elements = elements(),
            pageAnchors = emptyList(),
            anchorId = "p3",
            fallbackIndex = 1,
        )

        assertEquals(0, result.anchorIndex)
        assertEquals(null, result.anchorId)
        assertEquals(0, result.targetElementIndex)
    }

    @Test
    fun `given viewport crosses later paragraph when resolve visible position then uses paragraph anchor and relative offset`() {
        val result = resolveVisibleReaderPosition(
            elements = elements(),
            pageAnchorElementIndex = 0,
            elementOffsets = listOf(
                ReaderElementOffset(elementIndex = 0, y = 0),
                ReaderElementOffset(elementIndex = 1, y = 80),
                ReaderElementOffset(elementIndex = 2, y = 220),
            ),
            pageScrollOffset = 250,
        )

        assertEquals("p2", result.anchorId)
        assertEquals(2, result.paragraphIndex)
        assertEquals(30, result.anchorOffset)
    }

    @Test
    fun `given viewport before first text when resolve visible position then falls back to page anchor`() {
        val result = resolveVisibleReaderPosition(
            elements = elements(),
            pageAnchorElementIndex = 0,
            elementOffsets = listOf(
                ReaderElementOffset(elementIndex = 0, y = 0),
                ReaderElementOffset(elementIndex = 1, y = 80),
            ),
            pageScrollOffset = 40,
        )

        assertEquals("h1", result.anchorId)
        assertEquals(0, result.paragraphIndex)
        assertEquals(40, result.anchorOffset)
    }

    private fun elements(): List<ContentElement> = listOf(
        ContentElement.Heading(level = 1, text = "标题", anchorId = "h1"),
        ContentElement.Paragraph(spans = listOf(TextSpan("第一段")), anchorId = "p1"),
        ContentElement.Paragraph(spans = listOf(TextSpan("第二段")), anchorId = "p2"),
        ContentElement.Paragraph(spans = listOf(TextSpan("第三段")), anchorId = "p3"),
    )

    private fun pageAnchors(): List<PageAnchor> = listOf(
        PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0),
        PageAnchor(elementIndex = 2, textOffset = 0, pageIndex = 1),
        PageAnchor(elementIndex = 3, textOffset = 0, pageIndex = 2),
    )
}
