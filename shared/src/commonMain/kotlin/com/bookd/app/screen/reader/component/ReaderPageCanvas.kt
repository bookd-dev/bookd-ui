package com.bookd.app.screen.reader.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.translate
import com.bookd.app.basic.reader.ReaderEngine
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.extension.rememberImagePainterMap
import com.bookd.app.data.model.ContentElement

/**
 * 阅读器页面 Canvas 组件
 *
 * 性能优化：
 * - 使用 remember 缓存 renderCommands，只在翻页时重新测量
 * - 绘制阶段只做简单的 Canvas 操作，零布局计算
 */
@Composable
fun ReaderPageCanvas(
    renderCommands: List<RenderCommand>,
    pageAnchor: PageAnchor,
    nextPageAnchor: PageAnchor?,
    elements: List<ContentElement>,
    readerEngine: ReaderEngine,
    onLinkClick: (String) -> Unit,
    onFootnoteClick: (String) -> Unit,
    onImageClick: (String, String?) -> Unit,
    verticalOffset: Float = readerEngine.marginVerticalPx.toFloat(),
    modifier: Modifier = Modifier
) {
    val imagePainters = elements.rememberImagePainterMap()

    // 绘制阶段（使用 Canvas）
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pageAnchor) {
                awaitEachGesture {
                    awaitFirstDown(pass = PointerEventPass.Main)
                    val up = waitForUpOrCancellation(pass = PointerEventPass.Main) ?: return@awaitEachGesture

                    val rawOffset = up.position
                    val contentX = rawOffset.x - readerEngine.marginHorizontalPx.toFloat()
                    val contentY = rawOffset.y - verticalOffset

                    // 1. 检测图片点击
                    for (cmd in renderCommands) {
                        if (cmd is RenderCommand.Image) {
                            val contentWidth = size.width - 2 * readerEngine.marginHorizontalPx
                            val imageOffsetX = (contentWidth - cmd.width) / 2f
                            if (contentX >= imageOffsetX && contentX <= imageOffsetX + cmd.width &&
                                contentY >= cmd.y && contentY <= cmd.y + cmd.height) {
                                up.consume()
                                onImageClick(cmd.src, cmd.altText)
                                return@awaitEachGesture
                            }
                        }
                    }

                    // 2. 检测文本 annotation（链接/脚注）
                    for (cmd in renderCommands) {
                        if (cmd is RenderCommand.Text) {
                            val textTop = cmd.y.toFloat()
                            val textBottom = textTop + cmd.textLayout.size.height
                            if (contentY in textTop..<textBottom) {
                                val localOffset = androidx.compose.ui.geometry.Offset(contentX, contentY - textTop)
                                val charOffset = cmd.textLayout.getOffsetForPosition(localOffset)

                                // 检查 URL annotation
                                val urlAnnotations = cmd.textLayout.layoutInput.text
                                    .getStringAnnotations(tag = "URL", start = charOffset, end = charOffset)
                                if (urlAnnotations.isNotEmpty()) {
                                    up.consume()
                                    onLinkClick(urlAnnotations.first().item)
                                    return@awaitEachGesture
                                }

                                // 检查 footnote annotation（文本型脚注）
                                val footnoteAnnotations = cmd.textLayout.layoutInput.text
                                    .getStringAnnotations(tag = "footnote", start = charOffset, end = charOffset)
                                if (footnoteAnnotations.isNotEmpty()) {
                                    up.consume()
                                    onFootnoteClick(footnoteAnnotations.first().item)
                                    return@awaitEachGesture
                                }

                                // 检查 inlineContent 脚注图标（图片占位符）
                                val inlineContentMap = cmd.inlineContent ?: continue
                                for ((key, info) in inlineContentMap) {
                                    val rect = cmd.textLayout.placeholderRects.getOrNull(info.index) ?: continue
                                    if (contentX >= rect.left && contentX <= rect.right &&
                                        contentY - textTop >= rect.top && contentY - textTop <= rect.bottom) {
                                        // key 格式: "footnote:{footnoteId}:{image}:{index}"
                                        val parts = key.split(":")
                                        if (parts.size >= 2 && parts[0] == "footnote") {
                                            up.consume()
                                            onFootnoteClick(parts[1])
                                            return@awaitEachGesture
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // 未命中任何内容 → 不消费事件，透传给外层
                }
            },
        onDraw = {
            translate(left = readerEngine.marginHorizontalPx.toFloat(), top = verticalOffset) {
                renderCommands.forEach { command ->
                    readerEngine.draw(
                        drawScope = this,
                        renderCommand = command,
                        imagePainters = imagePainters
                    )
                }
            }
        }
    )
}