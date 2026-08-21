package com.bookd.app.basic.reader.controller

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.sp
import org.junit.Test
import kotlin.test.assertEquals

class ParagraphInlineContentCollectorTest {

    @Test
    fun `given split text when placeholders are adjusted then inline indices match visible placeholders`() {
        val collector = ParagraphInlineContentCollector()
        val placeholder = Placeholder(
            width = 1.sp,
            height = 1.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
        )
        collector["first"] = ParagraphInlineContentInfo(
            id = "first",
            src = "first.png",
            index = 0,
            range = AnnotatedString.Range(placeholder, start = 2, end = 3),
        )
        collector["second"] = ParagraphInlineContentInfo(
            id = "second",
            src = "second.png",
            index = 1,
            range = AnnotatedString.Range(placeholder, start = 5, end = 6),
        )
        val text = AnnotatedString("abcdef")

        val placeholders = collector.getAdjustPlaceholder(text, startOffset = 4)
        val inlineContent = collector.getAllInlineContent(text, startOffset = 4).orEmpty()

        assertEquals(1, placeholders.size)
        assertEquals(1, placeholders.single().start)
        assertEquals(2, placeholders.single().end)
        assertEquals(0, inlineContent.getValue("second").index)
        assertEquals("second.png", inlineContent.getValue("second").src)
    }
}
