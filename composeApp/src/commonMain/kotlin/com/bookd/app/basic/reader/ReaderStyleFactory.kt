package com.bookd.app.basic.reader

import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ReaderSettings

class ReaderStyleFactory(
    private val density: Density,
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

    // 3. 辅助：计算段间距 (px)
    // 这是一个坑：TextMeasurer 不直接支持 paragraphSpacing。
    // 我们需要在 measure 循环中手动添加这部分高度。
    val paragraphSpacingPx: Float by lazy {
        with(density) {
            settings.paragraphSpacing.dp.toPx()
        }
    }
}