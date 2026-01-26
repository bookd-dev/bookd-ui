package com.bookd.app.basic.reader.controller.styles

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ReaderSettings

class ReaderParagraphStyles(private val settings: ReaderSettings) {

    val headlineParagraphStyle: ParagraphStyle = getHeadlineParagraphStyle()

    val bodyParagraphStyle: ParagraphStyle = getBodyParagraphStyle()

    /**
     * 正文段落样式 (影响排版布局)
     */
    private fun getBodyParagraphStyle(): ParagraphStyle {
        // 首行缩进逻辑：通常中文缩进两个字符宽度
        val indentAmount = if (settings.firstLineIndent) {
            (settings.fontSize * 2).sp
        } else {
            0.sp
        }

        return ParagraphStyle(
            textAlign = settings.getTextAlign(),
            textIndent = TextIndent(firstLine = indentAmount),
            lineBreak = LineBreak.Paragraph,
            // 注意：Compose 这里的 lineHeight 是指段落内的行高，通常由 TextStyle 覆盖
            // 我们主要用 ParagraphStyle 控制对齐和缩进
        )
    }

    private fun getHeadlineParagraphStyle(): ParagraphStyle {
        return ParagraphStyle(
            textAlign = if (settings.getTextAlign() == TextAlign.Center) TextAlign.Center else TextAlign.Left,
            textIndent = TextIndent.None, //标题不需要缩紧
            lineBreak = LineBreak.Heading,
        )
    }
}