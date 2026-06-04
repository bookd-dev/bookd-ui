package com.bookd.app.basic.reader.controller.styles

import androidx.compose.ui.graphics.Color

data class ReaderThemeColors(
    val background: Color = Color.White,
    val content: Color = Color.Black,
    val secondaryContent: Color = Color(0xFF616161),
    val surfaceVariant: Color = Color(0xFFF5F5F5),
    val outline: Color = Color(0xFF9E9E9E),
    val outlineVariant: Color = Color(0xFFE0E0E0),
)
