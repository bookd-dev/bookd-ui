package com.bookd.app.basic.reader.controller.styles

import androidx.compose.ui.graphics.Color

class ReaderColorStyles(
    colors: ReaderThemeColors = ReaderThemeColors(),
) {
    val codeBlockBackground: Color = colors.surfaceVariant
    val codeBlockBorder: Color = colors.outlineVariant
    val dividerLine: Color = colors.outlineVariant
    val quoteBar: Color = colors.outline
}
