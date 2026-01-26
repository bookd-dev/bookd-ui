package com.bookd.app.basic.reader.controller

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.bookd.app.basic.reader.controller.styles.ReaderParagraphStyles
import com.bookd.app.basic.reader.controller.styles.ReaderSizeStyles
import com.bookd.app.basic.reader.controller.styles.ReaderTextStyles
import com.bookd.app.data.model.ReaderSettings
import com.bookd.app.data.model.TextSpan
import com.bookd.app.data.model.TextStyle as BookTextStyle


class ReaderStyleController(
    settings: ReaderSettings
) {

    /**
     * spanStyle 缓存, key是哈希值
     */
    private val spanStyleMemoryCache = mutableMapOf<Int, SpanStyle>()

    /**
     * 阅读器所需的textStyle集合
     */
    val textStyles: ReaderTextStyles = ReaderTextStyles(settings)

    /**
     * 阅读器所需的spacing/size集合
     */
    val sizeStyles: ReaderSizeStyles = ReaderSizeStyles(settings)

    /**
     * 阅读器所需的paragraphStyle集合
     */
    val paragraphStyles: ReaderParagraphStyles = ReaderParagraphStyles(settings)

    /**
     * 构建span样式文本
      */
    fun buildMeasureSpanStyle(
        span: TextSpan,
        style: TextStyle = textStyles.bodyTextStyle,
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