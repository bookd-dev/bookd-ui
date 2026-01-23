package com.bookd.app.basic.reader.data

import androidx.compose.ui.text.TextLayoutResult

// 渲染指令：这是测量后的产物，直接告诉 Canvas 画什么
sealed class RenderCmd {
    abstract val y: Int // 在当前页的 Y 坐标

    data class Text(
        override val y: Int,
        val textLayout: TextLayoutResult // 核心：缓存这个对象
    ) : RenderCmd()

    data class Image(
        override val y: Int,
        val src: String,
        val width: Int,
        val height: Int
    ) : RenderCmd()
}