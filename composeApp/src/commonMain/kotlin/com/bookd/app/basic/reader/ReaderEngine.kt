package com.bookd.app.basic.reader

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.bookd.app.data.model.ReaderSettings

class ReaderEngine(
    val textMeasurer: TextMeasurer,
    val density: Density,
    val settings: ReaderSettings,
    val constraints: Constraints, // 屏幕实际宽高
){
    private val styleFactory = ReaderStyleFactory(density, settings)



}