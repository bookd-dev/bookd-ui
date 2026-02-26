package com.bookd.app.basic.reader.factory

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import coil3.compose.AsyncImagePainter
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.factory.internal.shouldAddTopSpacing
import com.bookd.app.data.model.ContentElement

class QuoteElementFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density
) : IContentMeasureFactory<ContentElement.Quote, RenderCommand.Quote> {

    private val lineSpacing = styleController.sizeStyles.getLineSpacingPx(density)

    private val quoteVerticalPadding = lineSpacing / 2
    private val quoteLeftBarWidth = lineSpacing / 4
    private val quoteLeftBarSpacing = lineSpacing * 3 / 4

    override fun measure(
        elements: List<ContentElement>,
        element: ContentElement.Quote,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int
    ): MeasureResult {
        // 1. 计算顶部间距
        val topSpacing = if (shouldAddTopSpacing(elements, element, usedHeight)) quoteVerticalPadding else 0

        // 2. 检查是否有足够空间
        if (topSpacing > availableHeight) {
            return MeasureResult.NEXT
        }

        // 3. 构建带样式的文本（使用 quoteTextStyle）
        val text = buildAnnotatedString {
            withStyle(styleController.paragraphStyles.bodyParagraphStyle) {
                element.spans.forEach { span ->
                    withStyle(styleController.buildMeasureSpanStyle(span, styleController.textStyles.quoteTextStyle)) {
                        append(span.text)
                    }
                }
            }
        }

        // 4. 截取需要测量的部分
        val textToMeasure = if (startOffset > 0) text.subSequence(startOffset, text.length) else text

        // 5. 测量文本高度（需要减去左侧竖线和间距的宽度）
        val result = textMeasurer.measure(
            text = textToMeasure,
            constraints = Constraints(maxWidth = contentWidth - quoteLeftBarWidth - quoteLeftBarSpacing)
        )
        val textHeight = result.size.height

        // 6. 计算总高度（顶部间距 + 文本高度 + 底部间距）
        val totalHeight = topSpacing + textHeight + quoteVerticalPadding

        // 7. 判断是否可以完整显示
        return if (totalHeight <= availableHeight) {
            MeasureResult(
                measuredHeight = totalHeight,
                isSplit = false,
                nextOffset = 0
            )
        } else {
            // 8. 计算可用高度（已扣除顶部间距）
            val remainingHeight = availableHeight - topSpacing

            // 9. 计算可以显示的行数
            val lastVisibleLine = result.getLineForVerticalPosition(remainingHeight.toFloat())
            val lineBottom = result.getLineBottom(lastVisibleLine)

            // 10. 严谨判断：如果这一行其实已经超出边界，是否应该挤到下一页？
            val splitLine = if (lineBottom > remainingHeight) maxOf(0, lastVisibleLine - 1) else lastVisibleLine

            // 11. 如果连第一行都放不下，说明空间极小
            val splitOffsetSub = result.getLineEnd(splitLine, visibleEnd = true)
            return if (splitOffsetSub == 0) {
                // 第一行就超了，整体移到下一页
                MeasureResult.NEXT
            } else {
                MeasureResult(
                    measuredHeight = availableHeight,
                    isSplit = true,
                    nextOffset = startOffset + splitOffsetSub
                )
            }
        }
    }

    override fun prerender(
        elements: List<ContentElement>,
        element: ContentElement.Quote,
        index: Int,
        startOffset: Int,
        endOffset: Int?,
        currentY: Int
    ): RenderCommand.Quote {
        var y = currentY
        val topSpacing = if (shouldAddTopSpacing(elements, element, y)) quoteVerticalPadding else 0
        y += topSpacing

        val text = buildAnnotatedString {
            withStyle(styleController.paragraphStyles.bodyParagraphStyle) {
                element.spans.forEach { span ->
                    withStyle(styleController.buildMeasureSpanStyle(span, styleController.textStyles.quoteTextStyle)) {
                        append(span.text)
                    }
                }
            }
        }

        val start = startOffset
        val end = endOffset ?: text.length
        val textToRender = text.subSequence(start, end)

        val result = textMeasurer.measure(
            text = textToRender,
            constraints = Constraints(maxWidth = contentWidth - quoteLeftBarWidth - quoteLeftBarSpacing)
        )

        val totalHeight = topSpacing + result.size.height + quoteVerticalPadding

        return RenderCommand.Quote(
            y = y,
            textLayout = result,
            height = totalHeight
        )
    }

    override fun draw(
        drawScope: DrawScope,
        imagePainters: Map<String, AsyncImagePainter>,
        command: RenderCommand.Quote
    ) {
        val barTop = command.y.toFloat()
        val barHeight = command.textLayout.size.height + quoteVerticalPadding
        drawScope.drawRect(
            color = styleController.colorStyles.quoteBar,
            topLeft = Offset(0f, barTop),
            size = Size(quoteLeftBarWidth.toFloat(), barHeight.toFloat())
        )

        drawScope.drawText(
            textLayoutResult = command.textLayout,
            topLeft = Offset((quoteLeftBarWidth + quoteLeftBarSpacing).toFloat(), command.y.toFloat())
        )
    }
}
