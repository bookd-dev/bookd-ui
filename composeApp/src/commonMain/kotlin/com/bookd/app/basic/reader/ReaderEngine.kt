package com.bookd.app.basic.reader

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.basic.reader.factory.ContentElementFactory
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
    private val contentWidth: Int = styleController.sizeStyles.getContentWidth(constraints.maxWidth, density)
    private val contentHeight: Int = styleController.sizeStyles.getContentHeight(constraints.maxHeight, density)

    // 3. 辅助：计算段间距 (px)
    // 这是一个坑：TextMeasurer 不直接支持 paragraphSpacing。
    // 我们需要在 measure 循环中手动添加这部分高度。
    private val spacingPx: Int = styleController.sizeStyles.getLineSpacingPx(density)

    private val factory = ContentElementFactory(contentWidth, contentHeight, textMeasurer, styleController, density)


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
            val startOffset = if (i == currentAnchor.elementIndex) currentAnchor.textOffset else 0

            val (measuredHeight, isSplit, nextOffset) = measureElement(
                elements = elements,
                element = element,
                isStartElement = (i != currentAnchor.elementIndex || currentAnchor.textOffset == 0),
                startOffset = startOffset,
                usedHeight = currentY, //已经使用的高度
                availableHeight = contentHeight - currentY //剩余可用高度
            )

            // 处理特殊情况：元素高度为 0，但标记为需要分页
            if (measuredHeight == 0 && isSplit) {
                // 强制分页，不消耗当前页高度
                val nextAnchor = PageAnchor(i, 0, anchors.last().pageIndex + 1)
                anchors.add(nextAnchor)
                currentAnchor = nextAnchor
                currentY = 0
                continue
            }

            // 处理分页逻辑
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
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int,
    ): MeasureResult {
        val factory = factory.getMeasureElementFactory(element) ?: return MeasureResult.SKIP

        return factory.measure(
            elements = elements,
            element = element,
            isStartElement = isStartElement,
            startOffset = startOffset,
            usedHeight = usedHeight,
            availableHeight = availableHeight
        )
    }
}