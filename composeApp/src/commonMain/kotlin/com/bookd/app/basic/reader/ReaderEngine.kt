package com.bookd.app.basic.reader

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.basic.reader.factory.ContentElementFactory
import com.bookd.app.basic.reader.factory.ParagraphMeasureFactory
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings

class ReaderEngine(
    val textMeasurer: TextMeasurer,
    val density: Density,
    val constraints: Constraints, // 屏幕实际宽高
    val settings: ReaderSettings,
){
    private val styleController = ReaderStyleController(settings)

    // 计算内容区域的有效宽高
    val contentWidth: Int = constraints.maxWidth - with(density) { (settings.marginHorizontal * 2).dp.roundToPx() }
    val contentHeight: Int = constraints.maxHeight - with(density) { (settings.marginVertical * 2).dp.roundToPx() }

    // 3. 辅助：计算段间距 (px)
    // 这是一个坑：TextMeasurer 不直接支持 paragraphSpacing。
    // 我们需要在 measure 循环中手动添加这部分高度。
    val spacingPx: Int = with(density) { settings.paragraphSpacing.dp.roundToPx() }


    /**
     * 计算分页锚点
     */
    fun calculatePageAnchors(elements: List<ContentElement>): List<PageAnchor> {
        val anchors = mutableListOf<PageAnchor>()
        var currentAnchor = PageAnchor(0, 0, 0)
        anchors.add(currentAnchor)

        var currentY = 0 // 当前页已占用的高度
        var i = 0

        while (i < elements.size) {
            val element = elements[i]
            val isStartOfElement = (i != currentAnchor.elementIndex || currentAnchor.textOffset == 0)

            // 段间距逻辑：
            // 1. 只在段落开头（isStartOfElement）添加
            // 2. 页面第一行（currentY == 0）不添加（避免顶部间距）
            // 3. 被分割段落的后半部分在新页面也不添加（段间距应在段落间）
            if (isStartOfElement && currentY > 0 && element is ContentElement.Paragraph) {
                if (currentY + spacingPx > contentHeight) {
                    // 加上间距就超了，直接分页
                    val nextAnchor = PageAnchor(i, 0, anchors.last().pageIndex + 1)
                    anchors.add(nextAnchor)
                    currentAnchor = nextAnchor
                    currentY = 0
                    continue // 重新处理这个元素
                } else {
                    currentY += spacingPx
                }
            }

            // 2. 测量元素
            val startOffset = if (i == currentAnchor.elementIndex) currentAnchor.textOffset else 0

            val (measuredHeight, isSplit, nextOffset) = measureElement(
                elements = elements,
                element = element,
                startOffset = startOffset,
                availableHeight = contentHeight - currentY //剩余可用高度
            )

            // 3. 处理分页逻辑
            if (isSplit) {
                currentY = 0 // 新页高度重置
                val nextAnchor = PageAnchor(i, nextOffset, anchors.last().pageIndex + 1)
                anchors.add(nextAnchor)
                currentAnchor = nextAnchor
                // 注意：这里不 i++，因为下一页还要处理这个元素的剩余部分
            } else {
                currentY += measuredHeight
                i++
            }
        }
        return anchors
    }

    private fun measureElement(
        elements: List<ContentElement>,
        element: ContentElement,
        startOffset: Int,
        availableHeight: Int,
    ): MeasureResult {
        val factory = getMeasureElementFactory(element)
        return factory.measure(elements, element, startOffset, availableHeight)
    }

    @Suppress("UNCHECKED_CAST")
    private fun getMeasureElementFactory(element: ContentElement): ContentElementFactory<ContentElement> {
        return when(element) {
            is ContentElement.Paragraph -> ParagraphMeasureFactory(contentWidth, contentHeight, textMeasurer, styleController, density)
            is ContentElement.Code -> TODO()
            ContentElement.Divider -> TODO()
            is ContentElement.Footnote -> TODO()
            is ContentElement.Heading -> TODO()
            is ContentElement.Image -> TODO()
            is ContentElement.ListBlock -> TODO()
            is ContentElement.Quote -> TODO()
        } as ContentElementFactory<ContentElement>
    }
}