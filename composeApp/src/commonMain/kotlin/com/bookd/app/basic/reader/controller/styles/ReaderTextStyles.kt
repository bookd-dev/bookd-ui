package com.bookd.app.basic.reader.controller.styles

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ReaderSettings

class ReaderTextStyles(private val settings: ReaderSettings) {

    // 正文文本大小, 影响全局的测量
    val bodyTextStyle: TextStyle = TextStyle(
        fontFamily = settings.getFontFamily(),
        fontSize = settings.fontSize.sp,
        fontWeight = FontWeight(settings.fontWeight),
        lineHeight = (settings.fontSize * settings.lineHeight).sp,
        letterSpacing = settings.letterSpacing.sp,
        textAlign = settings.getTextAlign(),
        // 优化行高对齐，防止文字切头去尾
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        ),
    )

    //脚注内容，只影响脚注
    val footnoteTextStyle: TextStyle = bodyTextStyle.copy(
        fontSize = (settings.fontSize - 2).sp,
        lineHeight = ((settings.fontSize - 2) * settings.lineHeight).sp,
    )

    //图片alt内容
    val imageAlternateTextStyle: TextStyle = bodyTextStyle.copy(
        fontSize = (settings.fontSize - 4).sp,
        lineHeight = ((settings.fontSize - 4) * settings.lineHeight).sp,
    )

    //代码内容
    val codeTextStyle: TextStyle = bodyTextStyle.copy(
        fontFamily = FontFamily.Monospace, //代码强制使用 monospace
        fontSize = (settings.fontSize - 2).sp,
        lineHeight = ((settings.fontSize - 2) * settings.lineHeight).sp,
    )

    // 引用内容
    val quoteTextStyle: TextStyle = bodyTextStyle.copy(
        fontStyle = FontStyle.Italic, //引用强制斜体
        textDecoration = TextDecoration.combine(
            listOf(TextDecoration.Underline, TextDecoration.LineThrough),
        ) //强制下划线
    )

    /**
     * 获取 Heading 的 TextStyle
     * @param level 标题级别 1-6
     */
    fun getHeaderTextStyle(level: Int): TextStyle {
        val baseSize = settings.fontSize
        val fontSize = when (level) {
            1 -> (baseSize * 1.8).toInt()
            2 -> (baseSize * 1.5).toInt()
            3 -> (baseSize * 1.3).toInt()
            4 -> (baseSize * 1.15).toInt()
            5 -> (baseSize * 1.05).toInt()
            else -> baseSize //level6或者高于与正文同大小
        }.sp

        val fontWeight = when (level) {
            1, 2 -> FontWeight.Bold
            3, 4 -> FontWeight.SemiBold
            else -> FontWeight.Medium
        }

        return bodyTextStyle.copy(
            fontSize = fontSize,
            fontWeight = fontWeight,
            lineHeight = (fontSize.value * 1.2).sp,
        )
    }
}

