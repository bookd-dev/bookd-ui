package com.bookd.app.basic.reader

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TextSpan
import com.bookd.app.data.model.TextStyle as BookTextStyle

class ReaderStyleFactory(
    private val settings: ReaderSettings
) {

    // 1. 基础文本样式 (影响测量)
    val baseTextStyle: TextStyle by lazy {
        TextStyle(
            fontFamily = settings.getFontFamily(), // 需实现字体加载逻辑
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
    }

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
    fun buildSpanStyle(span: TextSpan): SpanStyle {
        val isBold = span.styles.contains(BookTextStyle.BOLD)
        val isItalic = span.styles.contains(BookTextStyle.ITALIC)
        val isCode = span.styles.contains(BookTextStyle.CODE)
        val hasLink = span.link != null

        return SpanStyle(
            fontSize = settings.fontSize.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight(settings.fontWeight),
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = buildTextDecoration(span.styles, hasLink),
//        color = if (hasLink) Color.Unspecified else textColor, //不影响测量大小，无视
            letterSpacing = settings.letterSpacing.sp,
            fontFamily = if (isCode) FontFamily.Monospace else null,
//        background = if (isCode) codeBackgroundColor else Color.Unspecified //不影响测量大小，无视
        )
    }


    /**
     * 构建文本装饰
     */
    private fun buildTextDecoration(styles: List<BookTextStyle>, hasLink: Boolean): TextDecoration? {
        val decorations = buildList {
            if (styles.contains(BookTextStyle.UNDERLINE) || hasLink) add(TextDecoration.Underline)
            if (styles.contains(BookTextStyle.STRIKETHROUGH)) add(TextDecoration.LineThrough)
        }
        return if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
    }
}