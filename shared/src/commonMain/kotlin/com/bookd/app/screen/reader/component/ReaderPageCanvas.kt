package com.bookd.app.screen.reader.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
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
    onParagraphLongClick: (anchorId: String?, paragraphIndex: Int) -> Unit,
    verticalOffset: Float = readerEngine.marginVerticalPx.toFloat(),
    modifier: Modifier = Modifier
) {
    val imagePainters = elements.rememberImagePainterMap()

    // 绘制阶段（使用 Canvas）
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(pageAnchor, nextPageAnchor, renderCommands, elements, readerEngine, verticalOffset) {
                val inlineFootnoteTouchPadding = 16.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown(pass = PointerEventPass.Main)
                    val pressResult = waitForReaderPress(down)
                    if (pressResult is ReaderPressResult.LongPress) {
                        val rawOffset = pressResult.position
                        val contentX = rawOffset.x - readerEngine.marginHorizontalPx.toFloat()
                        val contentY = rawOffset.y - verticalOffset
                        val hit = resolveReaderCanvasParagraphLongPress(
                            renderCommands = renderCommands,
                            elements = elements,
                            pageAnchor = pageAnchor,
                            nextPageAnchor = nextPageAnchor,
                            contentX = contentX,
                            contentY = contentY,
                        )
                        if (hit != null) {
                            pressResult.change.consume()
                            onParagraphLongClick(hit.anchorId, hit.paragraphIndex)
                            consumeUntilAllUp()
                        }
                        return@awaitEachGesture
                    }

                    val up = (pressResult as? ReaderPressResult.Tap)?.change
                        ?: return@awaitEachGesture

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
                            val inlineFootnoteId = findReaderCanvasInlineFootnote(
                                command = cmd,
                                contentX = contentX,
                                contentY = contentY,
                                touchPadding = inlineFootnoteTouchPadding,
                            )
                            if (inlineFootnoteId != null) {
                                up.consume()
                                onFootnoteClick(inlineFootnoteId)
                                return@awaitEachGesture
                            }

                            val textTop = cmd.y.toFloat()
                            val textBottom = textTop + cmd.textLayout.size.height
                            if (contentY in textTop..<textBottom) {
                                val localOffset = androidx.compose.ui.geometry.Offset(contentX, contentY - textTop)
                                val charOffset = cmd.textLayout.getOffsetForPosition(localOffset)

                                // 检查 URL annotation
                                val url = findReaderCanvasStringAnnotation(
                                    text = cmd.textLayout.layoutInput.text,
                                    tag = "URL",
                                    charOffset = charOffset,
                                )
                                if (url != null) {
                                    up.consume()
                                    onLinkClick(url)
                                    return@awaitEachGesture
                                }

                                // 检查 footnote annotation（文本型脚注）
                                val footnoteId = findReaderCanvasStringAnnotation(
                                    text = cmd.textLayout.layoutInput.text,
                                    tag = "footnote",
                                    charOffset = charOffset,
                                )
                                if (footnoteId != null) {
                                    up.consume()
                                    onFootnoteClick(footnoteId)
                                    return@awaitEachGesture
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

internal data class ReaderCanvasParagraphHit(
    val anchorId: String?,
    val paragraphIndex: Int,
)

internal fun findReaderCanvasStringAnnotation(
    text: AnnotatedString,
    tag: String,
    charOffset: Int,
): String? {
    if (text.length == 0) return null
    val safeOffset = charOffset.coerceIn(0, text.length - 1)
    return text.getStringAnnotations(
        tag = tag,
        start = safeOffset,
        end = safeOffset + 1,
    ).firstOrNull()?.item
}

internal fun parseReaderInlineFootnoteId(key: String): String? {
    if (!key.startsWith("footnote:")) return null
    val payload = key.removePrefix("footnote:")
    return payload.substringBefore(':').takeIf { it.isNotBlank() }
}

internal fun findReaderCanvasInlineFootnote(
    command: RenderCommand.Text,
    contentX: Float,
    contentY: Float,
    touchPadding: Float = 8f,
): String? {
    val inlineContentMap = command.inlineContent ?: return null
    for ((key, info) in inlineContentMap) {
        val rect = command.textLayout.placeholderRects.getOrNull(info.index) ?: continue
        val bounds = resolveReaderCanvasInlineBounds(
            commandY = command.y,
            rectLeft = rect.left,
            rectTop = rect.top,
            rectRight = rect.right,
            rectBottom = rect.bottom,
            touchPadding = touchPadding,
        )
        if (bounds.contains(contentX, contentY)) {
            parseReaderInlineFootnoteId(key)?.let { return it }
        }
    }
    return null
}

internal data class ReaderCanvasInlineBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }
}

internal fun resolveReaderCanvasInlineBounds(
    commandY: Int,
    rectLeft: Float,
    rectTop: Float,
    rectRight: Float,
    rectBottom: Float,
    touchPadding: Float = 8f,
): ReaderCanvasInlineBounds {
    return ReaderCanvasInlineBounds(
        left = rectLeft - touchPadding,
        top = commandY.toFloat() + rectTop - touchPadding,
        right = rectRight + touchPadding,
        bottom = commandY.toFloat() + rectBottom + touchPadding,
    )
}

internal fun resolveReaderCanvasParagraphLongPress(
    renderCommands: List<RenderCommand>,
    elements: List<ContentElement>,
    pageAnchor: PageAnchor,
    nextPageAnchor: PageAnchor?,
    contentX: Float,
    contentY: Float,
): ReaderCanvasParagraphHit? {
    if (contentX < 0f || contentY < 0f) return null

    var textCommandOrdinal = 0
    for (command in renderCommands) {
        if (command is RenderCommand.Text) {
            val textTop = command.y.toFloat()
            val textBottom = textTop + command.textLayout.size.height
            val textWidth = command.textLayout.size.width
            if (contentX <= textWidth && contentY in textTop..<textBottom) {
                val elementIndex = command.elementIndex.takeIf {
                    it in elements.indices && elements[it] is ContentElement.Paragraph
                } ?: resolveReaderCanvasParagraphElementIndex(
                    elements = elements,
                    pageAnchor = pageAnchor,
                    nextPageAnchor = nextPageAnchor,
                    textCommandOrdinal = textCommandOrdinal,
                ) ?: return null
                return ReaderCanvasParagraphHit(
                    anchorId = elements[elementIndex].anchorId,
                    paragraphIndex = elementIndex,
                )
            }
            textCommandOrdinal += 1
        }
    }
    return null
}

internal fun resolveReaderCanvasParagraphElementIndex(
    elements: List<ContentElement>,
    pageAnchor: PageAnchor,
    nextPageAnchor: PageAnchor?,
    textCommandOrdinal: Int,
): Int? {
    if (elements.isEmpty() || textCommandOrdinal < 0) return null

    val startIndex = pageAnchor.elementIndex.coerceIn(0, elements.lastIndex)
    val endIndex = readerCanvasVisibleEndElementIndex(elements.lastIndex, nextPageAnchor)
    if (startIndex > endIndex) return null

    return (startIndex..endIndex)
        .filter { elements[it] is ContentElement.Paragraph }
        .getOrNull(textCommandOrdinal)
}

private fun readerCanvasVisibleEndElementIndex(
    lastElementIndex: Int,
    nextPageAnchor: PageAnchor?,
): Int {
    if (nextPageAnchor == null) return lastElementIndex
    val endIndex = if (nextPageAnchor.textOffset == 0) {
        nextPageAnchor.elementIndex - 1
    } else {
        nextPageAnchor.elementIndex
    }
    return endIndex.coerceIn(0, lastElementIndex)
}

private sealed interface ReaderPressResult {
    data class Tap(val change: PointerInputChange) : ReaderPressResult
    data class LongPress(val position: Offset, val change: PointerInputChange) : ReaderPressResult
    data object Canceled : ReaderPressResult
}

private suspend fun AwaitPointerEventScope.waitForReaderPress(
    down: PointerInputChange,
): ReaderPressResult {
    var result: ReaderPressResult = ReaderPressResult.Canceled
    var lastChange = down
    try {
        withTimeout(viewConfiguration.longPressTimeoutMillis) {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val changes = event.changes
                val currentChange = changes.firstOrNull { it.id == down.id } ?: changes.first()
                lastChange = currentChange

                if (changes.all { it.changedToUp() }) {
                    result = ReaderPressResult.Tap(changes.first())
                    break
                }

                if (changes.any { it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding) }) {
                    result = ReaderPressResult.Canceled
                    break
                }

                val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
                if (consumeCheck.changes.any { it.isConsumed }) {
                    result = ReaderPressResult.Canceled
                    break
                }
            }
        }
    } catch (_: PointerEventTimeoutCancellationException) {
        return ReaderPressResult.LongPress(lastChange.position, lastChange)
    }
    return result
}

private suspend fun AwaitPointerEventScope.consumeUntilAllUp() {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        event.changes.forEach { it.consume() }
        if (event.changes.all { it.changedToUp() || !it.pressed }) {
            return
        }
    }
}
