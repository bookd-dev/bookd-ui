package com.bookd.app.basic.reader.factory

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import coil3.compose.AsyncImagePainter
import com.bookd.app.basic.reader.controller.ParagraphInlineContentCollector
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.data.RenderInlineContentInfo
import com.bookd.app.basic.reader.factory.internal.autoAppendFootnoteInlineContent
import com.bookd.app.basic.reader.factory.internal.shouldAddTopSpacing
import com.bookd.app.data.model.ContentElement

class ParagraphElementFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density,
) : IContentMeasureFactory<ContentElement.Paragraph, RenderCommand.Text> {

    private val lineSpacing = styleController.sizeStyles.getLineSpacingPx(density)

    override fun measure(
        elements: List<ContentElement>,
        element: ContentElement.Paragraph,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int,
    ): MeasureResult {
        // 每次测量创建对象
        // 虽然会有对象创建开销，但是避免了这儿共享 [inlineContentPlaceholders] 可能导致的潜在线程安全问题
        val inlineContentCollector = ParagraphInlineContentCollector()

        // 1. 计算段落间间距（只在段落开头添加）
        val paragraphSpacing = if (shouldAddTopSpacing(elements, element, usedHeight)) lineSpacing else 0

        // 2. 检查是否有足够空间
        if (paragraphSpacing > availableHeight) {
            // 间距就超了，直接分页
            return MeasureResult.NEXT
        }

        // 3. 构建带样式的 AnnotatedString
        val text = element.toAnnotatedString(elements, startOffset, inlineContentCollector)

        // 4. 截取需要测量的部分
        val textToMeasure = if (startOffset > 0) text.subSequence(startOffset, text.length) else text

        val result = textMeasurer.measure(
            text = textToMeasure,
            constraints = Constraints(maxWidth = contentWidth), // 使用计算好的 contentWidth
            placeholders = inlineContentCollector.getAdjustPlaceholder(text, startOffset)
        )

        // 5. 计算可用高度（已扣除段间距）
        val remainingHeight = availableHeight - paragraphSpacing
        val textHeight = result.size.height

        if (textHeight <= remainingHeight) {
            return MeasureResult(
                measuredHeight = textHeight + paragraphSpacing,
                isSplit = false,
                nextOffset = 0,
            )
        } else {
            // 切割逻辑 (同之前)
            val lastVisibleLine = result.getLineForVerticalPosition(remainingHeight.toFloat())
            val lineBottom = result.getLineBottom(lastVisibleLine)

            // 严谨判断：如果这一行其实已经超出边界一点点，是否应该挤到下一页？
            // 对于阅读器，通常宁可空一点，不要截断文字下半截
            val splitLine = if (lineBottom > remainingHeight) maxOf(0, lastVisibleLine - 1) else lastVisibleLine

            // 如果连第一行都放不下（splitLine < 0 或 splitLine == 0 且超高），说明空间极小
            // 这种边缘情况需要特殊处理，防止死循环
            val splitOffsetSub = result.getLineEnd(splitLine, visibleEnd = true)
            return MeasureResult(
                measuredHeight = availableHeight,
                isSplit = true,
                nextOffset = startOffset + splitOffsetSub
            )
        }
    }


    override fun prerender(
        elements: List<ContentElement>,
        element: ContentElement.Paragraph,
        index: Int,
        startOffset: Int,
        endOffset: Int?,
        currentY: Int
    ): RenderCommand.Text {
        var y = currentY
        val inlineContentCollector = ParagraphInlineContentCollector()

        if (shouldAddTopSpacing(elements, element, y)) {
            y += lineSpacing
        }

        // 开始预渲染

        val text = element.toAnnotatedString(elements, startOffset, inlineContentCollector)
        // 截取需要测量的部分
        val end = endOffset ?: text.lastIndex
        val textToRender = text.subSequence(startOffset, end)

        val textLayoutResult = textMeasurer.measure(
            text = textToRender,
            constraints = Constraints(maxWidth = contentWidth),
            placeholders = inlineContentCollector.getAdjustPlaceholder(text, startOffset)
        )

        return RenderCommand.Text(
            y = y,
            textLayout = textLayoutResult,
            inlineContent = inlineContentCollector.getAllInlineContent()
        )
    }

    override fun draw(drawScope: DrawScope, imagePainters: Map<String, AsyncImagePainter>, command: RenderCommand.Text) {
        // 1. 绘制文本（包括脚注占位符 \uFFFC）
        drawScope.drawText(
            textLayoutResult = command.textLayout,
            topLeft = Offset(0f, command.y.toFloat()),
        )
        // 2. 绘制脚注图片占位符（如果有）
        command.inlineContent?.forEach { (_, inlineInfo) ->
            drawFootnoteInlineContent(
                drawScope = drawScope,
                imagePainters = imagePainters,
                command = command,
                info = inlineInfo
            )
        }
    }

    /**
     * 绘制单个脚注占位符（图片）
     */
    private fun drawFootnoteInlineContent(
        drawScope: DrawScope,
        imagePainters: Map<String, AsyncImagePainter>,
        command: RenderCommand.Text,
        info: RenderInlineContentInfo
    ) {
        // 获取占位符的位置和大小
        val placeholder = command.textLayout.placeholderRects.getOrNull(info.index) ?: return

        // 实际绘制脚注图片的逻辑
        val painter = imagePainters[info.src]

        if (painter != null) {
            drawScope.withTransform({
                translate(placeholder.left, placeholder.top)
            }) {
                // 利用 Painter 绘制，它内部处理了所有的 Crossfade 和状态
                with(painter) {
                    draw(Size(placeholder.width, placeholder.height))
                }
            }
        }
    }


    private fun ContentElement.Paragraph.toAnnotatedString(
        elements: List<ContentElement>,
        startOffset: Int,
        inlineContentCollector: ParagraphInlineContentCollector
    ): AnnotatedString {
        return buildAnnotatedString {
            // 应用段落样式（对齐、缩进）
            // 只有当这是段落的开头时，才应用缩进。如果是跨页的后半段，不应该缩进！
            val isParagraphStart = (startOffset == 0)
            val pStyle = styleController.paragraphStyles.bodyParagraphStyle.let {
                if (!isParagraphStart) it.copy(textIndent = TextIndent.None) else it
            }

            withStyle(pStyle) {
                // 这里应该遍历 element.spans 来应用局部样式（如加粗）
                // 简单起见，这里只 append 纯文本
                spans.forEach { span ->
                    withStyle(styleController.buildMeasureSpanStyle(span)) {
                        append(span.text)
                    }
                    // 会自动判定是否要添加脚注占位
                    autoAppendFootnoteInlineContent(
                        styleController = styleController,
                        density = density,
                        inlineCollector = inlineContentCollector,
                        elements = elements,
                        span = span
                    )
                }
            }
        }
    }
}