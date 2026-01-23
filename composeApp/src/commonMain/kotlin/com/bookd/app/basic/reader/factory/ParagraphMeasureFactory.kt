package com.bookd.app.basic.reader.factory

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.sp
import com.bookd.app.basic.reader.ReaderStyleFactory
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TextSpan
import com.bookd.app.data.model.TextStyle

class ParagraphMeasureFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleFactory: ReaderStyleFactory,
) : ContentElementFactory<ContentElement.Paragraph> {


    override fun measure(
        element: ContentElement.Paragraph,
        startOffset: Int,
        availableHeight: Int
    ): MeasureResult {
        // 关键步骤：构建带样式的 AnnotatedString
        val text = buildAnnotatedString {
            // 应用段落样式（对齐、缩进）
            // 只有当这是段落的开头时，才应用缩进。如果是跨页的后半段，不应该缩进！
            val isParagraphStart = (startOffset == 0)
            val pStyle = styleFactory.createParagraphStyle().let {
                if (!isParagraphStart) it.copy(textIndent = TextIndent.None) else it
            }

            withStyle(pStyle) {
                // 这里应该遍历 element.spans 来应用局部样式（如加粗）
                // 简单起见，这里只 append 纯文本
                element.spans.forEach { span ->
                    withStyle(styleFactory.buildSpanStyle(span)) {
                        append(span.text)
                    }
                }
            }
        }

        // 截取需要测量的部分
        val textToMeasure = if (startOffset > 0) text.subSequence(startOffset, text.length) else text

        val result = textMeasurer.measure(
            text = textToMeasure,
            constraints = Constraints(maxWidth = contentWidth) // 使用计算好的 contentWidth
        )

        if (result.size.height <= availableHeight) {
            return MeasureResult(
               measuredHeight = result.size.height,
               isSplit = false,
               nextOffset = 0
            )
        } else {
            // 切割逻辑 (同之前)
            val lastVisibleLine = result.getLineForVerticalPosition(availableHeight.toFloat())
            val lineBottom = result.getLineBottom(lastVisibleLine)

            // 严谨判断：如果这一行其实已经超出边界一点点，是否应该挤到下一页？
            // 对于阅读器，通常宁可空一点，不要截断文字下半截
            val splitLine = if (lineBottom > availableHeight) maxOf(0, lastVisibleLine - 1) else lastVisibleLine

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
}