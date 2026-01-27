package com.bookd.app.screen.reader.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.bookd.app.basic.reader.ReaderEngine
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.extension.getCommandHeight
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
    pageAnchor: PageAnchor,
    nextPageAnchor: PageAnchor?,
    elements: List<ContentElement>,
    readerEngine: ReaderEngine,
    onLinkClick: (String) -> Unit,
    onFootnoteClick: (String) -> Unit,
    onImageClick: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. 准备绘制指令（测量阶段）
    // 使用 remember 缓存，只在 pageAnchor 变化时重新计算
    val renderCommands = remember(pageAnchor, nextPageAnchor, elements) {
        readerEngine.prepareRenderCommands(pageAnchor, nextPageAnchor, elements)
    }

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
            renderCommands.forEach { command ->
                readerEngine.draw(
                    drawScope = this,
                    renderCommand = command,
                )
            }
        }
    )
}