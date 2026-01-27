package com.bookd.app.basic.reader.factory

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.factory.internal.shouldAddTopSpacing
import com.bookd.app.data.model.ContentElement

class ListBlockElementFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density
) : IContentMeasureFactory<ContentElement.ListBlock, RenderCommand.ListBlock> {
    private val listVerticalPadding = styleController.sizeStyles.getLineSpacingPx(density)
    private val listItemBottomPadding = listVerticalPadding / 4
    private val listItemStartPadding = listVerticalPadding

    override fun measure(
        elements: List<ContentElement>,
        element: ContentElement.ListBlock,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int
    ): MeasureResult {
        // 1. 计算顶部间距
        val topSpacing = if (shouldAddTopSpacing(elements, element, usedHeight)) listVerticalPadding else 0

        // 2. 检查是否有足够空间
        if (topSpacing > availableHeight) {
            return MeasureResult.NEXT
        }

        // 3. 计算每个列表项的高度
        val listItemHeights = element.items.map { item ->
            // 测量列表项文本高度
            val text = buildAnnotatedString {
                item.spans.forEach { span ->
                    withStyle(styleController.buildMeasureSpanStyle(span)) {
                        append(span.text)
                    }
                }
            }
            val result = textMeasurer.measure(
                text = text,
                constraints = Constraints(maxWidth = contentWidth - listItemStartPadding)
            )
            result.size.height
        }

        // 4. 计算总高度（顶部间距 + 底部间距 + 列表项高度 + 列表项间距）
        val totalHeight = topSpacing +
            listVerticalPadding +
            listItemHeights.sumOf { it } +
            (listItemHeights.size - 1) * listItemBottomPadding

        // 5. 判断是否可以完整显示
        return if (totalHeight <= availableHeight) {
            MeasureResult(
                measuredHeight = totalHeight,
                isSplit = false,
                nextOffset = 0
            )
        } else {
            // 空间不足，整体移到下一页
            MeasureResult.NEXT
        }
    }
}