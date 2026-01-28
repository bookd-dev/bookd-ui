package com.bookd.app.basic.reader.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.IntSize

/**
 * 渲染指令基类
 */
sealed class RenderCommand {
    abstract val y: Int // 在当前页的 Y 坐标

    /**
     * 标题渲染指令
     */
    data class Heading(
        override val y: Int,
        val level: Int,
        val textLayout: TextLayoutResult
    ) : RenderCommand()

    /**
    * 文本渲染指令
    */
    data class Text(
        override val y: Int,
        val textLayout: TextLayoutResult, // 已测量的文本布局
        val inlineContent: Map<String, RenderInlineContentInfo>? = null // 脚注占位符
    ) : RenderCommand()

    /**
     * 图片渲染指令
     */
    data class Image(
        override val y: Int,
        val src: String,
        val width: Int,
        val height: Int,
        val imageBitmap: ImageBitmap? = null, // 缓存的图片
        val altText: String? = null,
        val altTextLayout: TextLayoutResult? = null // alt 文本的布局
    ) : RenderCommand()

    /**
     * 引用块渲染指令
     */
    data class Quote(
        override val y: Int,
        val textLayout: TextLayoutResult
    ) : RenderCommand()

    /**
     * 代码块渲染指令
     */
    data class Code(
        override val y: Int,
        val textLayout: TextLayoutResult,
    ) : RenderCommand()


    /**
     * 列表子项渲染指令
     */
    data class ListItem(
        override val y: Int,
        val prefix: String, // "1." 或 "•"
        val textLayout: TextLayoutResult,
        val prefixLayout: TextLayoutResult
    ) : RenderCommand()

    /**
     * 列表渲染指令
     */
    data class ListBlock(
        override val y: Int,
        val items: List<ListItem>,
    ) : RenderCommand()


    /**
     * 分隔线渲染指令
     */
    data class Divider(
        override val y: Int,
        val height: Int,
    ) : RenderCommand()

    /**
     * 脚注渲染指令
     */
    data class Footnote(
        override val y: Int,
        val footnoteId: String,
        val markerLayout: TextLayoutResult, // "[1]" 标记
        val contentLayouts: List<TextLayoutResult> // 脚注内容
    ) : RenderCommand()
}

/**
 * 文本片段样式信息（用于点击交互）
 */
data class RenderSpanStyleInfo(
    val range: IntRange,
    val style: SpanStyle,
    val annotation: String? = null, // "link:http://..." 或 "footnote:fn1"
    val annotationType: String? = null // "link" 或 "footnote"
)

/**
 * 行内内容信息（脚注图片占位符）
 */
data class RenderInlineContentInfo(
    val id: String,
    val placeholder: Placeholder,
    val size: IntSize,
    val index: Int,
    val src: String? = null,
)