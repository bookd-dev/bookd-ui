package com.bookd.app.basic.reader.data

import androidx.compose.ui.text.TextLayoutResult
import kotlinx.serialization.Serializable

// 渲染指令：这是测量后的产物，直接告诉 Canvas 画什么

sealed class RenderCommand {
    abstract val y: Int // 在当前页的 Y 坐标


}