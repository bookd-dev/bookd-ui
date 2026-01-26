package com.bookd.app.basic.reader.factory

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.factory.internal.shouldAddTopSpacing
import com.bookd.app.data.model.ContentElement

class HeadlineMeasureFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density
) : IContentElementFactory<ContentElement.Heading> {


    override fun measure(
        elements: List<ContentElement>,
        element: ContentElement.Heading,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int
    ): MeasureResult {
        // 1. 计算间距
        val topSpacing = if (shouldAddTopSpacing(elements, element, usedHeight)) styleController.sizeStyles.getHeadingTopSpacing(element.level) else 0
        val bottomSpacing = styleController.sizeStyles.getHeadingBottomSpacing(element.level)
        val totalSpace = topSpacing + bottomSpacing

        // 2. 构建带样式的文本
        val text = buildAnnotatedString {
            withStyle(styleController.paragraphStyles.headlineParagraphStyle) {
                withStyle(styleController.textStyles.getHeaderTextStyle(element.level).toSpanStyle()) {
                    append(element.text)
                }
            }
        }

        // 3. 测量文本高度
        val textResult = textMeasurer.measure(
            text = text,
            constraints = Constraints(maxWidth = contentWidth)
        )
        val textHeight = textResult.size.height

        // 4. 计算总高度 (文本 + 上下间距)
        val totalHeight = textHeight + totalSpace

        // 5. 判断是否可以完整显示(不分页策略)
        return if (totalHeight <= availableHeight) {
            MeasureResult(
                measuredHeight = totalHeight,
                isSplit = false,
                nextOffset = 0
            )
        } else {
            // 无法完整显示，整体移到下一页
            // 返回 0 高度，让 ReaderEngine 知道需要新页面
            MeasureResult.NEXT
        }
    }
}