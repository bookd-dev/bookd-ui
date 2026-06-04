package com.bookd.app.screen.reader

import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.data.model.BookManifest
import com.bookd.app.data.model.BookManifestDocument
import com.bookd.app.data.model.BookMetadata
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.TextSpan
import com.bookd.app.data.model.TocItem
import com.bookd.app.screen.reader.component.findReaderCanvasStringAnnotation
import com.bookd.app.screen.reader.component.parseReaderInlineFootnoteId
import com.bookd.app.screen.reader.component.resolveReaderCanvasInlineBounds
import com.bookd.app.screen.reader.component.resolveReaderCanvasParagraphElementIndex
import androidx.compose.ui.text.buildAnnotatedString
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ReaderInlineInteractionsTest {

    @Test
    fun `given active chapter elements when resolve footnote then returns matching footnote`() {
        val footnote = ContentElement.Footnote(
            footnoteId = "fn-1",
            contentSpans = listOf(TextSpan("Footnote body")),
            anchorId = "fn-anchor",
        )
        val elements = listOf(
            ContentElement.Paragraph(
                spans = listOf(TextSpan(text = "Body", footnoteId = "fn-1")),
                anchorId = "p-1",
            ),
            footnote,
        )

        assertSame(footnote, resolveReaderFootnote(elements, "fn-1"))
        assertNull(resolveReaderFootnote(elements, "missing"))
        assertEquals("Footnote body", footnote.readerDisplayText())
    }

    @Test
    fun `given platform opens link when open external link then reports opened`() {
        var openedUrl: String? = null

        val result = openReaderExternalLink("https://example.test") { url ->
            openedUrl = url
        }

        assertIs<ReaderExternalLinkResult.Opened>(result)
        assertEquals("https://example.test", openedUrl)
    }

    @Test
    fun `given platform rejects link when open external link then returns fallback url`() {
        val result = openReaderExternalLink("bad-url") {
            throw IllegalArgumentException("unsupported")
        }

        val fallback = assertIs<ReaderExternalLinkResult.Fallback>(result)
        assertEquals("bad-url", fallback.url)
    }

    @Test
    fun `given paragraph selection when build bookmark request then preserves dispatch context`() {
        val request = buildReaderParagraphBookmarkRequest(
            selection = ReaderParagraphSelection(
                chapterIndex = 4,
                anchorId = "ch4-p2",
                paragraphIndex = 7,
                scrollOffset = 0,
            ),
            note = "note",
        )

        assertEquals(4, request.chapterIndex)
        assertEquals("ch4-p2", request.anchorId)
        assertEquals(7, request.paragraphIndex)
        assertEquals(0, request.scrollOffset)
        assertEquals("note", request.note)
    }

    @Test
    fun `given image preview transform when zoom and pan then updates bounded state`() {
        val zoomed = updateReaderImagePreviewTransform(
            current = ReaderImagePreviewTransform(),
            zoomChange = 2f,
            panX = 12f,
            panY = -8f,
        )
        val reset = updateReaderImagePreviewTransform(
            current = zoomed,
            zoomChange = 0.1f,
            panX = 100f,
            panY = 100f,
        )
        val toggled = toggleReaderImagePreviewZoom(ReaderImagePreviewTransform())

        assertEquals(2f, zoomed.scale)
        assertEquals(12f, zoomed.offsetX)
        assertEquals(-8f, zoomed.offsetY)
        assertEquals(ReaderImagePreviewTransform(), reset)
        assertEquals(2.5f, toggled.scale)
    }

    @Test
    fun `given canvas text annotations when click character then resolves footnote and link`() {
        val text = buildAnnotatedString {
            append("go note")
            addStringAnnotation(tag = "URL", annotation = "https://example.test", start = 0, end = 2)
            addStringAnnotation(tag = "footnote", annotation = "fn-1", start = 3, end = 7)
        }

        assertEquals("https://example.test", findReaderCanvasStringAnnotation(text, "URL", 0))
        assertEquals("fn-1", findReaderCanvasStringAnnotation(text, "footnote", 4))
        assertEquals("fn-1", findReaderCanvasStringAnnotation(text, "footnote", text.length))
        assertNull(findReaderCanvasStringAnnotation(text, "URL", 4))
    }

    @Test
    fun `given inline footnote key when parse id then returns footnote id`() {
        assertEquals("fn-1", parseReaderInlineFootnoteId("footnote:fn-1:https://example.test/a.png:0"))
        assertEquals("fn-1", parseReaderInlineFootnoteId("footnote:fn-1:4:https://example.test/a.png"))
        assertNull(parseReaderInlineFootnoteId("image:fn-1"))
        assertNull(parseReaderInlineFootnoteId("footnote::0"))
    }

    @Test
    fun `given inline placeholder rect when resolve bounds then adds command y and touch padding`() {
        val bounds = resolveReaderCanvasInlineBounds(
            commandY = 120,
            rectLeft = 10f,
            rectTop = 6f,
            rectRight = 24f,
            rectBottom = 22f,
            touchPadding = 4f,
        )

        assertEquals(6f, bounds.left)
        assertEquals(122f, bounds.top)
        assertEquals(28f, bounds.right)
        assertEquals(146f, bounds.bottom)
        assertTrue(bounds.contains(10f, 126f))
        assertFalse(bounds.contains(10f, 121f))
    }

    @Test
    fun `given epub internal chapter link when resolve internal link then returns document target`() {
        val manifest = manifest(
            documents = listOf(
                BookManifestDocument(index = 0, href = "Text/nav.xhtml", anchorPrefix = "epub:nav#"),
                BookManifestDocument(index = 1, href = "Text/Chapter_1.xhtml", anchorPrefix = "epub:ch1#"),
            )
        )

        val target = resolveReaderInternalLink(
            url = "Chapter_1.xhtml#Intro",
            manifest = manifest,
            currentChapterIndex = 0,
        )

        requireNotNull(target)
        assertEquals(1, target.chapterIndex)
        assertEquals("epub:ch1#intro", target.anchorId)
        assertEquals("Text/Chapter_1.xhtml", resolveReaderRelativeHref("Chapter_1.xhtml", "Text/nav.xhtml"))
    }

    @Test
    fun `given epub zero based chapter href when resolve internal link then matches manifest document`() {
        val manifest = manifest(
            documents = listOf(
                BookManifestDocument(index = 0, href = "OPS/chapter0.xhtml", anchorPrefix = "epub:ch0#"),
                BookManifestDocument(index = 3, href = "OPS/nav.xhtml", anchorPrefix = "epub:nav#"),
            )
        )

        val target = resolveReaderInternalLink(
            url = "chapter0.xhtml#Start",
            manifest = manifest,
            currentChapterIndex = 3,
        )

        requireNotNull(target)
        assertEquals(0, target.chapterIndex)
        assertEquals("epub:ch0#start", target.anchorId)
    }

    @Test
    fun `given epub relative mixed case href when resolve internal link then matches normalized document`() {
        val manifest = manifest(
            documents = listOf(
                BookManifestDocument(index = 0, href = "OPS/nav.xhtml", anchorPrefix = "epub:nav#"),
                BookManifestDocument(index = 2, href = "Text/Part0001.xhtml", anchorPrefix = "epub:part1#"),
            )
        )

        val target = resolveReaderInternalLink(
            url = "../Text/part0001.xhtml#Top",
            manifest = manifest,
            currentChapterIndex = 0,
        )

        requireNotNull(target)
        assertEquals(2, target.chapterIndex)
        assertEquals("epub:part1#top", target.anchorId)
    }

    @Test
    fun `given current chapter fragment link when resolve internal link then targets current document anchor`() {
        val manifest = manifest(
            documents = listOf(
                BookManifestDocument(index = 2, href = "OPS/chapter2.xhtml", anchorPrefix = "epub:ch2#"),
            )
        )

        val target = resolveReaderInternalLink(
            url = "#Section-2",
            manifest = manifest,
            currentChapterIndex = 2,
        )

        requireNotNull(target)
        assertEquals(2, target.chapterIndex)
        assertEquals("epub:ch2#section-2", target.anchorId)
    }

    @Test
    fun `given old manifest and numbered chapter link when resolve internal link then falls back to spine index`() {
        val oneBased = resolveReaderInternalLink(
            url = "Chapter_2.xhtml",
            manifest = manifest(documents = emptyList(), spine = listOf(0, 4, 8)),
            currentChapterIndex = 0,
        )
        val zeroBased = resolveReaderInternalLink(
            url = "chapter0.xhtml",
            manifest = manifest(documents = emptyList(), spine = listOf(0, 4, 8)),
            currentChapterIndex = 4,
        )
        val namedPart = resolveReaderInternalLink(
            url = "part0001.xhtml",
            manifest = manifest(documents = emptyList(), spine = listOf(0, 4, 8)),
            currentChapterIndex = 0,
        )
        val numericFile = resolveReaderInternalLink(
            url = "0003.xhtml",
            manifest = manifest(documents = emptyList(), spine = listOf(0, 4, 8)),
            currentChapterIndex = 0,
        )

        requireNotNull(oneBased)
        requireNotNull(zeroBased)
        requireNotNull(namedPart)
        requireNotNull(numericFile)
        assertEquals(4, oneBased.chapterIndex)
        assertEquals(0, zeroBased.chapterIndex)
        assertEquals(0, namedPart.chapterIndex)
        assertEquals(8, numericFile.chapterIndex)
        assertNull(oneBased.anchorId)
    }

    @Test
    fun `given reader links when classify then only external schemes open outside`() {
        assertTrue(isReaderExternalLink("https://example.test"))
        assertTrue(isReaderExternalLink("mailto:reader@example.test"))
        assertTrue(isReaderExternalLink("ftp://example.test/chapter.xhtml"))
        assertTrue(isReaderExternalLink("//example.test/chapter.xhtml"))
        assertFalse(isReaderExternalLink("Chapter_1.xhtml"))
        assertTrue(isReaderInternalDocumentLink("Chapter_1.xhtml"))
        assertTrue(isReaderInternalDocumentLink("#intro"))
        assertFalse(isReaderInternalDocumentLink("https://example.test/chapter.xhtml"))
        assertFalse(isReaderInternalDocumentLink("ftp://example.test/chapter.xhtml"))
        assertFalse(isReaderInternalDocumentLink("//example.test/chapter.xhtml"))
    }

    @Test
    fun `given visible page range when resolve canvas paragraph element then maps text command ordinal`() {
        val elements = listOf(
            ContentElement.Heading(level = 1, text = "Title", anchorId = "title"),
            ContentElement.Paragraph(listOf(TextSpan("First")), anchorId = "p-1"),
            ContentElement.Image(src = "image.png", anchorId = "image"),
            ContentElement.Paragraph(listOf(TextSpan("Second")), anchorId = "p-2"),
            ContentElement.Footnote(footnoteId = "fn", contentSpans = listOf(TextSpan("Fn"))),
            ContentElement.Paragraph(listOf(TextSpan("Third")), anchorId = "p-3"),
        )

        val first = resolveReaderCanvasParagraphElementIndex(
            elements = elements,
            pageAnchor = PageAnchor(elementIndex = 1, textOffset = 0, pageIndex = 0),
            nextPageAnchor = PageAnchor(elementIndex = 5, textOffset = 0, pageIndex = 1),
            textCommandOrdinal = 0,
        )
        val second = resolveReaderCanvasParagraphElementIndex(
            elements = elements,
            pageAnchor = PageAnchor(elementIndex = 1, textOffset = 0, pageIndex = 0),
            nextPageAnchor = PageAnchor(elementIndex = 5, textOffset = 0, pageIndex = 1),
            textCommandOrdinal = 1,
        )
        val outside = resolveReaderCanvasParagraphElementIndex(
            elements = elements,
            pageAnchor = PageAnchor(elementIndex = 1, textOffset = 0, pageIndex = 0),
            nextPageAnchor = PageAnchor(elementIndex = 5, textOffset = 0, pageIndex = 1),
            textCommandOrdinal = 2,
        )

        assertEquals(1, first)
        assertEquals(3, second)
        assertNull(outside)
    }

    @Test
    fun `given split next page anchor when resolve canvas paragraph element then includes split paragraph`() {
        val elements = listOf(
            ContentElement.Paragraph(listOf(TextSpan("First")), anchorId = "p-1"),
            ContentElement.Image(src = "image.png", anchorId = "image"),
            ContentElement.Paragraph(listOf(TextSpan("Second")), anchorId = "p-2"),
        )

        val second = resolveReaderCanvasParagraphElementIndex(
            elements = elements,
            pageAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0),
            nextPageAnchor = PageAnchor(elementIndex = 2, textOffset = 8, pageIndex = 1),
            textCommandOrdinal = 1,
        )

        assertEquals(2, second)
    }

    private fun manifest(
        documents: List<BookManifestDocument>,
        spine: List<Int> = documents.map { it.index },
    ): BookManifest {
        return BookManifest(
            id = 1,
            title = "Book",
            author = null,
            format = "epub",
            totalChapters = spine.size,
            toc = spine.map { TocItem(index = it, title = "Chapter $it") },
            spine = spine,
            metadata = BookMetadata(),
            documents = documents,
        )
    }
}
