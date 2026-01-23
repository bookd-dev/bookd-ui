package com.bookd.app.basic.reader.controller

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TextSpan
import com.bookd.app.settings
import com.bookd.app.data.model.TextStyle as BookTextStyle


class ReaderStyleController(
    private val settings: ReaderSettings
) {

    /**
     * spanStyle 缓存, key是哈希值
     */
    private val spanStyleMemoryCache = mutableMapOf<Int, SpanStyle>()

    /**
     * 阅读器所需的styles集合
     */
    val styles: ReaderStyles = ReaderStyles(settings)

    // 2. 段落样式 (影响排版布局)
    fun createParagraphStyle(): ParagraphStyle {
        // 首行缩进逻辑：通常中文缩进两个字符宽度
        val indentAmount = if (settings.firstLineIndent) {
            (settings.fontSize * 2).sp
        } else {
            0.sp
        }

        return ParagraphStyle(
            textAlign = when (settings.textAlign) {
                "justify" -> TextAlign.Justify
                "center" -> TextAlign.Center
                "right" -> TextAlign.Right
                else -> TextAlign.Left
            },
            textIndent = TextIndent(firstLine = indentAmount),
            // 注意：Compose 这里的 lineHeight 是指段落内的行高，通常由 TextStyle 覆盖
            // 我们主要用 ParagraphStyle 控制对齐和缩进
        )
    }


    /**
     * 构建span样式文本
      */
    fun buildMeasureSpanStyle(
        span: TextSpan,
        style: TextStyle = styles.bodyTextStyle,
    ): SpanStyle {
        val hashCode = span.toMeasureHashCode(style)
        return spanStyleMemoryCache.getOrPut(hashCode) {
            span.toSpanStyle(style)
        }
    }

    /**
     * 根据可变内容生成hashCode
     */
    private fun TextSpan.toMeasureHashCode(style: TextStyle): Int {
        var result = style.fontSize.hashCode()
        result = 31 * result + style.fontWeight.hashCode()
        result = 31 * result + style.lineHeight.hashCode()
        result = 31 * result + styles.hashCode()
        return result
    }

    /**
     * 转换成 [SpanStyle]
     */
    private fun TextSpan.toSpanStyle(style: TextStyle): SpanStyle {
        val isBold = styles.contains(BookTextStyle.BOLD)
        val isItalic = styles.contains(BookTextStyle.ITALIC)
        val isCode = styles.contains(BookTextStyle.CODE)

        return SpanStyle(
            fontSize = style.fontSize,
            letterSpacing = style.letterSpacing,
            fontWeight = if (isBold) FontWeight.Bold else style.fontWeight,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            fontFamily = if (isCode) FontFamily.Monospace else null,
        )
    }
}

class ReaderStyles(settings: ReaderSettings) {

    // 正文文本大小, 影响全局的测量
    val bodyTextStyle: TextStyle = TextStyle(
        fontFamily = settings.getFontFamily(),
        fontSize = settings.fontSize.sp,
        fontWeight = FontWeight(settings.fontWeight),
        lineHeight = (settings.fontSize * settings.lineHeight).sp,
        letterSpacing = settings.letterSpacing.sp,
        // 优化行高对齐，防止文字切头去尾
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

    val footnoteTextStyle: TextStyle = bodyTextStyle.copy(
        fontSize = (settings.fontSize - 2).sp,
        lineHeight = ((settings.fontWeight - 2) * settings.lineHeight).sp,
    )
}