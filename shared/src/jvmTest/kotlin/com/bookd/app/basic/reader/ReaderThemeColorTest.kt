package com.bookd.app.basic.reader

import androidx.compose.ui.graphics.Color
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.controller.styles.ReaderThemeColors
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TextSpan
import org.junit.Test
import kotlin.test.assertEquals

class ReaderThemeColorTest {

    @Test
    fun `given dark reader theme colors when styles are built then text uses readable content color`() {
        val colors = ReaderThemeColors(
            background = Color(0xFF111315),
            content = Color(0xFFF4F6F8),
            secondaryContent = Color(0xFFE1E5EA),
            surfaceVariant = Color(0xFF252A2E),
            outline = Color(0xFF89919A),
            outlineVariant = Color(0xFF555D66),
        )
        val controller = ReaderStyleController(ReaderSettings(), colors)

        assertEquals(colors.content, controller.textStyles.bodyTextStyle.color)
        assertEquals(colors.content, controller.textStyles.footnoteTextStyle.color)
        assertEquals(colors.content, controller.textStyles.imageAlternateTextStyle.color)
        assertEquals(colors.content, controller.textStyles.codeTextStyle.color)
        assertEquals(colors.content, controller.textStyles.getHeaderTextStyle(level = 1).color)
        assertEquals(colors.content, controller.buildMeasureSpanStyle(TextSpan("正文")).color)
    }

    @Test
    fun `given dark reader theme colors when color styles are built then block chrome uses theme roles`() {
        val colors = ReaderThemeColors(
            background = Color(0xFF111315),
            content = Color(0xFFF4F6F8),
            secondaryContent = Color(0xFFE1E5EA),
            surfaceVariant = Color(0xFF252A2E),
            outline = Color(0xFF89919A),
            outlineVariant = Color(0xFF555D66),
        )
        val controller = ReaderStyleController(ReaderSettings(), colors)

        assertEquals(colors.surfaceVariant, controller.colorStyles.codeBlockBackground)
        assertEquals(colors.outlineVariant, controller.colorStyles.codeBlockBorder)
        assertEquals(colors.outlineVariant, controller.colorStyles.dividerLine)
        assertEquals(colors.outline, controller.colorStyles.quoteBar)
    }
}
