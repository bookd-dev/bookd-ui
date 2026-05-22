package com.bookd.app.basic.reader

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.extension.getCommandHeight
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TextSpan
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 图片 alt 文本与下一张图片重叠问题的回归测试
 *
 * 问题根因：getCommandHeight() 计算 Image 高度时漏掉了 altSpacing（图片与 alt 文本之间的间距），
 * 导致下一个元素的 y 坐标偏小，与 alt 文本发生视觉重叠。
 */
class ImageAltOverlapTest {

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
        val constraints = Constraints.fixed(pageWidth, pageHeight)
        val density = Density(1f)
        return ReaderEngine(
            textMeasurer = createTextMeasurer(),
            density = density,
            constraints = constraints,
            settings = settings,
        )
    }

    private fun image(
        src: String = "test.jpg",
        alt: String? = null,
        width: Int = 400,
        height: Int = 300
    ): ContentElement.Image = ContentElement.Image(
        src = src,
        alt = alt,
        width = width,
        height = height,
        aspectRatio = width.toDouble() / height
    )

    private fun paragraph(text: String): ContentElement.Paragraph =
        ContentElement.Paragraph(spans = listOf(TextSpan(text = text)))

    // ============ RenderCommand.Image altSpacing 字段测试 ============

    @Test
    fun `RenderCommand Image - 有 alt 文本时 altSpacing 应大于 0`() {
        val engine = createEngine()
        val elements = listOf(image(alt = "图片描述文字"))
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        val imageCmd = commands.filterIsInstance<RenderCommand.Image>().firstOrNull()
        assertNotNull(imageCmd, "应生成 Image 渲染指令")
        assertNotNull(imageCmd.altTextLayout, "有 alt 文本时 altTextLayout 不应为 null")
        assertTrue(imageCmd.altSpacing > 0, "有 alt 文本时 altSpacing 应大于 0，实际: ${imageCmd.altSpacing}")
    }

    @Test
    fun `RenderCommand Image - 无 alt 文本时 altSpacing 应为 0`() {
        val engine = createEngine()
        val elements = listOf(image(alt = null))
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        val imageCmd = commands.filterIsInstance<RenderCommand.Image>().firstOrNull()
        assertNotNull(imageCmd, "应生成 Image 渲染指令")
        assertEquals(0, imageCmd.altSpacing, "无 alt 文本时 altSpacing 应为 0")
    }

    // ============ getCommandHeight 高度计算测试 ============

    @Test
    fun `getCommandHeight - 有 alt 文本的 Image 高度应包含 altSpacing`() {
        val engine = createEngine()
        val elements = listOf(image(alt = "这是图片的 alt 说明文字"))
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        val imageCmd = commands.filterIsInstance<RenderCommand.Image>().firstOrNull()
        assertNotNull(imageCmd, "应生成 Image 渲染指令")

        val totalHeight = getCommandHeight(imageCmd)
        val expectedMinHeight = imageCmd.height + imageCmd.altSpacing + (imageCmd.altTextLayout?.size?.height ?: 0)

        assertEquals(
            expectedMinHeight, totalHeight,
            "Image 总高度应等于 图片高度 + altSpacing + alt文本高度"
        )
        // 确保 altSpacing 被纳入高度计算（不为 0）
        assertTrue(
            totalHeight > imageCmd.height + (imageCmd.altTextLayout?.size?.height ?: 0),
            "Image 总高度应大于 图片高度 + alt文本高度（差值为 altSpacing=${imageCmd.altSpacing}）"
        )
    }

    @Test
    fun `getCommandHeight - 无 alt 文本的 Image 高度等于图片本身高度`() {
        val engine = createEngine()
        val elements = listOf(image(alt = null))
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        val imageCmd = commands.filterIsInstance<RenderCommand.Image>().firstOrNull()
        assertNotNull(imageCmd, "应生成 Image 渲染指令")

        val totalHeight = getCommandHeight(imageCmd)
        assertEquals(
            imageCmd.height, totalHeight,
            "无 alt 文本时 Image 总高度应等于图片本身高度"
        )
    }

    // ============ 连续两张图片 y 坐标不重叠测试 ============

    @Test
    fun `连续两张有 alt 文本的图片 - 第二张图片 y 坐标不应与第一张 alt 文本重叠`() {
        val engine = createEngine(pageWidth = 800, pageHeight = 2000)
        val elements = listOf(
            image(src = "img1.jpg", alt = "第一张图片的 alt 说明文字", width = 600, height = 300),
            image(src = "img2.jpg", alt = "第二张图片的 alt 说明文字", width = 600, height = 300),
        )
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        val imageCommands = commands.filterIsInstance<RenderCommand.Image>()
        assertTrue(imageCommands.size >= 2, "应生成至少 2 个 Image 渲染指令，实际: ${imageCommands.size}")

        val img1 = imageCommands[0]
        val img2 = imageCommands[1]

        // 第一张图片 alt 文本的底部 y 坐标
        val img1AltTextBottom = img1.y + img1.height + img1.altSpacing + (img1.altTextLayout?.size?.height ?: 0)
        // 第二张图片的起始 y 坐标
        val img2Top = img2.y

        assertTrue(
            img2Top >= img1AltTextBottom,
            "第二张图片的起始 y(${img2Top}) 不应小于第一张图片 alt 文本的底部 y(${img1AltTextBottom})，否则会发生重叠"
        )
    }

    @Test
    fun `连续两张图片（第一张有 alt 文本）- 第二张 y 坐标不应与 alt 文本重叠`() {
        val engine = createEngine(pageWidth = 800, pageHeight = 2000)
        val elements = listOf(
            image(src = "img1.jpg", alt = "这是第一张图片的长 alt 说明文字，用于确保 alt 文本高度不为零", width = 600, height = 200),
            image(src = "img2.jpg", alt = null, width = 600, height = 200),
        )
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        val imageCommands = commands.filterIsInstance<RenderCommand.Image>()
        assertTrue(imageCommands.size >= 2, "应生成至少 2 个 Image 渲染指令，实际: ${imageCommands.size}")

        val img1 = imageCommands[0]
        val img2 = imageCommands[1]

        val img1BottomWithAlt = img1.y + img1.height + img1.altSpacing + (img1.altTextLayout?.size?.height ?: 0)
        val img2Top = img2.y

        assertTrue(
            img2Top >= img1BottomWithAlt,
            "第二张图片起始 y(${img2Top}) 不应小于第一张图片（含 alt 文本）的底部 y(${img1BottomWithAlt})"
        )
    }

    @Test
    fun `图片后紧跟段落 - 段落 y 坐标不应与图片 alt 文本重叠`() {
        val engine = createEngine(pageWidth = 800, pageHeight = 2000)
        val elements = listOf(
            image(src = "img1.jpg", alt = "这是图片的 alt 说明文字", width = 600, height = 300),
            paragraph("图片后面的段落内容。"),
        )
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        val imageCmd = commands.filterIsInstance<RenderCommand.Image>().firstOrNull()
        val textCmd = commands.filterIsInstance<RenderCommand.Text>().firstOrNull()

        assertNotNull(imageCmd, "应生成 Image 渲染指令")
        assertNotNull(textCmd, "应生成 Text 渲染指令")

        val imgAltBottom = imageCmd.y + imageCmd.height + imageCmd.altSpacing + (imageCmd.altTextLayout?.size?.height ?: 0)
        val textTop = textCmd.y

        assertTrue(
            textTop >= imgAltBottom,
            "段落起始 y(${textTop}) 不应小于图片（含 alt 文本）的底部 y(${imgAltBottom})"
        )
    }
}
