package com.bookd.app.screen.reader.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
    // 在 Composable 层预创建 Painter (这只是引用，不产生大对象)
    val imagePainters = elements.rememberImagePainterMap()

    // 2. 绘制阶段（使用 Canvas）
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(renderCommands) {
                detectTapGestures { offset ->
                    //TODO 点击事件
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