package com.bookd.app.basic.reader.factory

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ParagraphInlineContentCollector
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.factory.internal.autoAppendFootnoteInlineContent
import com.bookd.app.data.model.ContentElement

class ParagraphMeasureFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density,
) : IContentElementFactory<ContentElement.Paragraph> {

    private val inlineContentCollector = ParagraphInlineContentCollector()


    override fun measure(
        elements: List<ContentElement>,
        element: ContentElement.Paragraph,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int,
    ): MeasureResult {
        // 1. 计算段落间间距（只在段落开头添加）
        val paragraphSpacing = if (
            shouldAddParagraphSpacing(
                elements = elements,
                elementIndex = elements.indexOf(element),
                isStartOfElement = isStartElement,
                usedHeight = usedHeight
            ))
        {
            styleController.spacingStyles.getLineSpacingPx(density)
        } else {
            0
        }

        // 2. 检查是否有足够空间
        if (paragraphSpacing > availableHeight) {
            // 间距就超了，直接分页
            return MeasureResult.NEXT
        }

        // 3. 构建带样式的 AnnotatedString
        val text = buildAnnotatedString {
            // 应用段落样式（对齐、缩进）
            // 只有当这是段落的开头时，才应用缩进。如果是跨页的后半段，不应该缩进！
            val isParagraphStart = (startOffset == 0)
            val pStyle = styleController.paragraphStyles.bodyParagraphStyle.let {
                if (!isParagraphStart) it.copy(textIndent = TextIndent.None) else it
            }

            withStyle(pStyle) {
                // 这里应该遍历 element.spans 来应用局部样式（如加粗）
                // 简单起见，这里只 append 纯文本
                element.spans.forEach { span ->
                    withStyle(styleController.buildMeasureSpanStyle(span)) {
                        append(span.text)
                    }
                    //会自动判定是否要添加脚注占位
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

        // 4. 截取需要测量的部分
        val textToMeasure = if (startOffset > 0) text.subSequence(startOffset, text.length) else text

        val result = textMeasurer.measure(
            text = textToMeasure,
            constraints = Constraints(maxWidth = contentWidth) // 使用计算好的 contentWidth
        )

        // 5. 计算可用高度（已扣除段间距）
        val remainingHeight = availableHeight - paragraphSpacing
        val textHeight = result.size.height

        if (textHeight <= remainingHeight) {
            return MeasureResult(
                measuredHeight = textHeight + paragraphSpacing,
                isSplit = false,
                nextOffset = 0
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

    /**
     * 是否应该添加段落间隔
     */
    private fun shouldAddParagraphSpacing(
        elements: List<ContentElement>,
        elementIndex: Int,
        isStartOfElement: Boolean,
        usedHeight: Int
    ): Boolean {
        val element = elements[elementIndex]

        // 1. 只在段落开头添加
        if (!isStartOfElement || element !is ContentElement.Paragraph) return false

        // 2. 页面第一行（currentY == 0）不添加（避免顶部间距）
        if (usedHeight == 0) return false

        // 3. 标题后面跟段落时，通常不需要额外的段间距
        if (elementIndex > 0 && elements[elementIndex - 1] is ContentElement.Heading) {
            return false
        }

        // 4. 被分割段落的后半部分在新页面也不添加
        if (elementIndex > 0 && elements[elementIndex - 1] is ContentElement.Paragraph) {
            // 简化判断：如果前一个元素是段落，则当前段落应该有间距
            return true
        }

        return true
    }
}