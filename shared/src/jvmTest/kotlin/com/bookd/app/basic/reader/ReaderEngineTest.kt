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
import kotlin.test.assertTrue

/**
 * ReaderEngine 单元测试
 * 直接在 JVM 上构造 TextMeasurer（不依赖 Compose UI 测试环境）
 */
class ReaderEngineTest {

    // ============ 辅助函数 ============

    /** 创建不依赖 Compose 环境的 TextMeasurer */
    private fun createTextMeasurer(): TextMeasurer {
        return TextMeasurer(
            defaultFontFamilyResolver = createFontFamilyResolver(),
            defaultDensity = Density(1f),
            defaultLayoutDirection = LayoutDirection.Ltr,
        )
    }

    /** 创建标准 ReaderEngine（无 margin，方便测试高度计算） */
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

    /** 简单的单段落元素 */
    private fun paragraph(text: String): ContentElement.Paragraph =
        ContentElement.Paragraph(spans = listOf(TextSpan(text = text)))

    /** 标题元素 */
    private fun heading(level: Int = 1, text: String = "标题"): ContentElement.Heading =
        ContentElement.Heading(level = level, text = text)

    /** 分隔线 */
    private val divider: ContentElement = ContentElement.Divider()

    // ============ calculatePageAnchors 测试 ============

    @Test
    fun `calculatePageAnchors - 空元素列表返回包含单个初始锚点的列表`() {
        val engine = createEngine()
        val result = engine.calculatePageAnchors(emptyList())

        assertEquals(1, result.size, "空列表应该返回 1 个初始锚点")
        val anchor = result[0]
        assertEquals(0, anchor.elementIndex)
        assertEquals(0, anchor.textOffset)
        assertEquals(0, anchor.pageIndex)
    }

    @Test
    fun `calculatePageAnchors - 单个短段落不超页高只返回一个锚点`() {
        val engine = createEngine()
        val elements = listOf(paragraph("这是一段简短的文本内容。"))
        val result = engine.calculatePageAnchors(elements)

        assertEquals(1, result.size, "单个短段落不应该产生分页")
        assertEquals(0, result[0].pageIndex)
    }

    @Test
    fun `calculatePageAnchors - 超长文本应正确分多页`() {
        // 使用较小的页面高度以便于测试分页
        val engine = createEngine(pageHeight = 200)
        // 足够长的文本，在 200px 高度内必然分页
        val longText = "这是一段非常长的测试文本，用于测试分页逻辑是否正确。".repeat(20)
        val elements = listOf(paragraph(longText))
        val result = engine.calculatePageAnchors(elements)

        assertTrue(result.size > 1, "超长文本应产生多个页面锚点，实际得到: ${result.size}")
        // 验证页码连续
        result.forEachIndexed { index, anchor ->
            assertEquals(index, anchor.pageIndex, "页码应连续，index=$index")
        }
    }

    @Test
    fun `calculatePageAnchors - 超长文本分页后textOffset应连续递增`() {
        val engine = createEngine(pageHeight = 150)
        val longText = "文本内容文本内容文本内容文本内容文本内容".repeat(30)
        val elements = listOf(paragraph(longText))
        val result = engine.calculatePageAnchors(elements)

        assertTrue(result.size >= 2, "至少应该分 2 页")
        // 第一页从 offset=0 开始
        assertEquals(0, result[0].textOffset, "首页 textOffset 应为 0")
        // 后续页的 textOffset 应大于前一页（当在同一元素内分页时）
        for (i in 1 until result.size) {
            val prev = result[i - 1]
            val curr = result[i]
            if (curr.elementIndex == prev.elementIndex) {
                assertTrue(
                    curr.textOffset > prev.textOffset,
                    "第 ${i} 页的 textOffset(${curr.textOffset}) 应大于第 ${i - 1} 页的 textOffset(${prev.textOffset})"
                )
            }
        }
    }

    @Test
    fun `calculatePageAnchors - 混合多种元素类型应正确累积高度并分页`() {
        val engine = createEngine(pageHeight = 300)
        val elements = listOf(
            heading(1, "第一章 开始"),
            paragraph("第一段落内容。"),
            divider,
            paragraph("第二段落内容，稍微长一些的文本内容用于测试高度累积。".repeat(3)),
            heading(2, "第二节"),
            paragraph("第三段落内容。"),
        )
        val result = engine.calculatePageAnchors(elements)

        assertTrue(result.isNotEmpty(), "混合元素的锚点列表不应为空")
        // 验证锚点的 elementIndex 在合法范围内
        result.forEach { anchor ->
            assertTrue(
                anchor.elementIndex >= 0 && anchor.elementIndex <= elements.size,
                "elementIndex ${anchor.elementIndex} 应在合法范围 [0, ${elements.size}]"
            )
        }
    }

    @Test
    fun `calculatePageAnchors - Divider元素应正确处理不崩溃`() {
        val engine = createEngine()
        val elements = listOf(
            paragraph("前段落"),
            divider,
            paragraph("后段落"),
        )
        val result = engine.calculatePageAnchors(elements)

        assertTrue(result.isNotEmpty(), "含 Divider 的元素列表应返回合法锚点")
        assertEquals(0, result[0].pageIndex, "首页 pageIndex 应为 0")
    }

    @Test
    fun `calculatePageAnchors - 多页时锚点pageIndex从0开始连续递增`() {
        val engine = createEngine(pageHeight = 200)
        // 多个段落，确保跨多页
        val elements = (1..10).map { paragraph("第${it}段：这是测试文本内容，用于确保分页算法正确处理多段落场景。".repeat(3)) }
        val result = engine.calculatePageAnchors(elements)

        assertTrue(result.size >= 2, "应至少有 2 页才能验证 pageIndex 连续性")
        result.forEachIndexed { idx, anchor ->
            assertEquals(idx, anchor.pageIndex, "pageIndex 应从 0 连续递增，期望 $idx 得到 ${anchor.pageIndex}")
        }
    }

    @Test
    fun `calculatePageAnchors - 单个标题元素不超页高返回一个锚点`() {
        val engine = createEngine()
        val elements = listOf(heading(1, "第一章 标题"))
        val result = engine.calculatePageAnchors(elements)

        assertEquals(1, result.size, "单个标题不应产生分页")
        assertEquals(0, result[0].pageIndex)
        assertEquals(0, result[0].elementIndex)
    }

    // ============ prepareRenderCommands 测试 ============

    @Test
    fun `prepareRenderCommands - 有效锚点范围应返回非空渲染指令列表`() {
        val engine = createEngine()
        val elements = listOf(
            paragraph("第一段内容"),
            paragraph("第二段内容"),
            paragraph("第三段内容"),
        )
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        assertTrue(commands.isNotEmpty(), "有效锚点范围内应返回非空渲染指令")
    }

    @Test
    fun `prepareRenderCommands - 空elements应返回空列表`() {
        val engine = createEngine()
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, emptyList())

        assertTrue(commands.isEmpty(), "空 elements 应返回空渲染指令列表")
    }

    @Test
    fun `prepareRenderCommands - 首页锚点应正确渲染首页内容`() {
        val engine = createEngine()
        val elements = listOf(
            heading(1, "章节标题"),
            paragraph("正文内容"),
        )
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        assertTrue(commands.isNotEmpty(), "首页应有渲染指令")
        // 首页第一个指令应是标题
        val firstCommand = commands.first()
        assertTrue(
            firstCommand is RenderCommand.Heading,
            "首个指令应为 Heading，实际为: ${firstCommand::class.simpleName}"
        )
        // 所有指令的 y 坐标应 >= 0
        commands.forEach { cmd ->
            assertTrue(cmd.y >= 0, "渲染指令的 y 坐标应 >= 0，实际为: ${cmd.y}")
        }
    }

    @Test
    fun `prepareRenderCommands - 渲染指令y坐标应依次递增`() {
        val engine = createEngine()
        val elements = listOf(
            heading(1, "标题"),
            paragraph("第一段"),
            paragraph("第二段"),
            paragraph("第三段"),
        )
        val startAnchor = PageAnchor(elementIndex = 0, textOffset = 0, pageIndex = 0)
        val commands = engine.prepareRenderCommands(startAnchor, null, elements)

        assertTrue(commands.size >= 2, "应至少有 2 条渲染指令才能验证 y 递增")
        // y 坐标应依次非递减
        for (i in 1 until commands.size) {
            val prevY = commands[i - 1].y
            val currY = commands[i].y
            assertTrue(
                currY >= prevY,
                "渲染指令的 y 坐标应非递减: commands[$i].y($currY) < commands[${i - 1}].y($prevY)"
            )
        }
    }

    @Test
    fun `prepareRenderCommands - 中间页锚点应仅渲染该页范围内的元素`() {
        val engine = createEngine(pageHeight = 50)
        // 足够多的内容确保有多页
        val longText = "这是测试中间页渲染的长文本内容，用于确保产生多个页面。".repeat(10)
        val elements = listOf(
            paragraph(longText),
            paragraph("第二个段落"),
        )
        val allAnchors = engine.calculatePageAnchors(elements)

        assertTrue(allAnchors.size >= 2, "应该有至少 2 个锚点才能测试中间页，实际: ${allAnchors.size}")

        // 测试第二页（中间页）的渲染指令
        val pageIndex = 1
        val startAnchor = allAnchors[pageIndex]
        val endAnchor = if (pageIndex + 1 < allAnchors.size) allAnchors[pageIndex + 1] else null
        val commands = engine.prepareRenderCommands(startAnchor, endAnchor, elements)

        // 中间页的渲染指令不为空，且 y 坐标从 0 开始（相对页面）
        assertTrue(commands.isNotEmpty(), "中间页应有渲染指令")
        assertEquals(0, commands.first().y, "中间页第一条指令的 y 坐标应从 0 开始")
    }
}
