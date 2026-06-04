package com.bookd.app.basic.reader

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TextSpan
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ImageSizingTest {

    private fun createTextMeasurer(): TextMeasurer {
        return TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(),
            defaultDensity = Density(1f),
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
    }

    private fun createEngine(
        pageWidth: Int = 800,
        pageHeight: Int = 1200,
        settings: ReaderSettings = ReaderSettings(
            marginHorizontal = 0,
            marginVertical = 0,
            fontSize = 16,
            lineHeight = 1.5,
            paragraphSpacing = 8,
        )
    ): ReaderEngine {
        return ReaderEngine(
            textMeasurer = createTextMeasurer(),
            density = Density(1f),
            constraints = Constraints.fixed(pageWidth, pageHeight),
            settings = settings,
        )
    }

    private fun image(
        src: String = "image.jpg",
        width: Int,
        height: Int
    ): ContentElement.Image {
        return ContentElement.Image(
            src = src,
            width = width,
            height = height,
            aspectRatio = width.toDouble() / height
        )
    }

    private fun paragraph(text: String): ContentElement.Paragraph {
        return ContentElement.Paragraph(spans = listOf(TextSpan(text = text)))
    }

    @Test
    fun `given wide image on empty page when preparing render command then image width is bounded by content width`() {
        val engine = createEngine(pageWidth = 800, pageHeight = 1200)
        val elements = listOf(image(width = 1200, height = 600))

        val commands = engine.prepareRenderCommands(
            startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0),
            endAnchor = null,
            elements = elements
        )

        val imageCommand = commands.filterIsInstance<RenderCommand.Image>().firstOrNull()
        assertNotNull(imageCommand, "应生成 Image 渲染指令")
        assertEquals(800, imageCommand.width, "图片宽度不应超过内容区域宽度")
        assertEquals(400, imageCommand.height, "图片应按原始宽高比等比缩放")
    }

    @Test
    fun `given wide image after paragraph when preparing render command then image width is bounded by content width`() {
        val engine = createEngine(pageWidth = 800, pageHeight = 1200)
        val elements = listOf(
            paragraph("图片前面的正文。"),
            image(width = 1200, height = 300)
        )

        val commands = engine.prepareRenderCommands(
            startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0),
            endAnchor = null,
            elements = elements
        )

        val imageCommand = commands.filterIsInstance<RenderCommand.Image>().firstOrNull()
        assertNotNull(imageCommand, "应生成 Image 渲染指令")
        assertTrue(
            imageCommand.width <= 800,
            "段落后的图片宽度(${imageCommand.width}) 不应超过内容区域宽度"
        )
        assertEquals(200, imageCommand.height, "段落后的图片应按原始宽高比等比缩放")
    }
}
