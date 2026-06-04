package com.bookd.app.basic.reader

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import coil3.compose.AsyncImagePainter
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.controller.styles.ReaderThemeColors
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.extension.getCommandHeight
import com.bookd.app.basic.reader.factory.ContentElementFactory
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.ReaderSettings

class ReaderEngine(
    private val textMeasurer: TextMeasurer,
    private val density: Density,
    private val constraints: Constraints, // 屏幕实际宽高
    private val settings: ReaderSettings,
    private val colors: ReaderThemeColors = ReaderThemeColors(),
){
    val styleController: ReaderStyleController = ReaderStyleController(settings, colors)

    // 计算内容区域的有效宽高
    private val contentWidth: Int = styleController.sizeStyles.getContentWidth(constraints.maxWidth, density)
    private val contentHeight: Int = styleController.sizeStyles.getContentHeight(constraints.maxHeight, density)

    /** 水平 margin（px），用于绘制时偏移 */
    val marginHorizontalPx: Int = (constraints.maxWidth - contentWidth) / 2
    /** 垂直 margin（px），用于绘制时偏移 */
    val marginVerticalPx: Int = (constraints.maxHeight - contentHeight) / 2

    private val factory = ContentElementFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 计算分页锚点
     */
    fun calculatePageAnchors(elements: List<ContentElement>): List<PageAnchor> {
        val anchors = mutableListOf<PageAnchor>()
        var currentAnchor = PageAnchor(0, 0, 0)
        anchors.add(currentAnchor)

        var currentY = 0 // 当前页已占用的高度
        var i = 0

        while (i < elements.size) {
            val element = elements[i]
            val startOffset = if (i == currentAnchor.elementIndex) currentAnchor.textOffset else 0

            val (measuredHeight, isSplit, nextOffset) = measureElement(
                elements = elements,
                element = element,
                isStartElement = (i != currentAnchor.elementIndex || currentAnchor.textOffset == 0),
                startOffset = startOffset,
                usedHeight = currentY, //已经使用的高度
                availableHeight = contentHeight - currentY //剩余可用高度
            )


            // 处理特殊情况：元素高度为 0，但标记为需要分页
            if (measuredHeight == 0 && isSplit) {
                // 强制分页，不消耗当前页高度
                val nextAnchor = PageAnchor(i, 0, anchors.last().pageIndex + 1)
                anchors.add(nextAnchor)
                currentAnchor = nextAnchor
                currentY = 0
                continue
            }

            // 处理分页逻辑
            if (isSplit) {
                currentY = 0 // 新页高度重置
                val nextAnchor = PageAnchor(i, nextOffset, anchors.last().pageIndex + 1)
                anchors.add(nextAnchor)
                currentAnchor = nextAnchor
                // 注意：这里不 i++，因为下一页还要处理这个元素的剩余部分
            } else {
                currentY += measuredHeight
                i++
            }
        }
        return anchors
    }


    /**
     * 准备当前页的渲染指令列表（测量阶段）
     *
     * @param startAnchor 当前页的起始锚点
     * @param endAnchor 下一页的起始锚点（可选，用于确定当前页的结束位置）
     * @param elements 完整的章节元素列表
     * @return 渲染指令列表（已测量，可直接用于绘制）
     */
    fun prepareRenderCommands(
        startAnchor: PageAnchor,
        endAnchor: PageAnchor?,
        elements: List<ContentElement>
    ): List<RenderCommand> {
        val commands = mutableListOf<RenderCommand>()
        var currentY = 0

        // 计算遍历范围
        val endIndex = endAnchor?.elementIndex ?: elements.lastIndex
        val iterateEnd = if (endAnchor != null && endAnchor.textOffset == 0) {
            endIndex - 1
        } else {
            endIndex
        }

        // 遍历当前页的所有元素
        for (i in startAnchor.elementIndex..iterateEnd) {
            val element = elements[i]
            val command = renderElementCommand(
                elements = elements,
                element = element,
                index = i,
                startOffset = if (i == startAnchor.elementIndex) startAnchor.textOffset else 0,
                endOffset = if (endAnchor != null && i == endAnchor.elementIndex) endAnchor.textOffset else null,
                currentY = currentY
            )

            if (command != null) {
                val cmdH = getCommandHeight(command)
                commands.add(command)
                // Text 和 Image 的 getCommandHeight 不含 topSpacing（lineSpacing）
                // 而 command.y 由 prerender 内部正确累加了 spacing
                // 因此用 command.y + cmdH 代替 currentY + cmdH 来修正累积偏差
                // 对于 Heading/Quote/Code 等类型，height 字段已包含 topSpacing，
                // 但同样可以用 command.y + cmdH 来计算，因为对这些类型：
                //   command.y = prevCurrentY + topSpacing
                //   cmdH (= height) = topSpacing + textH + bottomSpacing
                //   command.y + cmdH 会多一个 topSpacing（错误）
                // 所以仅对 Text 和 Image 使用修正逻辑
                currentY = when (command) {
                    is RenderCommand.Text,
                    is RenderCommand.Image -> command.y + cmdH
                    else -> currentY + cmdH
                }
            } else {
            }
        }
        return commands
    }


    fun draw(drawScope: DrawScope, imagePainters: Map<String, AsyncImagePainter>, renderCommand: RenderCommand) {
        val factory = factory.getDrawElementFactory(renderCommand)
        factory?.draw(drawScope, imagePainters, renderCommand)
    }

    private fun measureElement(
        elements: List<ContentElement>,
        element: ContentElement,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int,
    ): MeasureResult {
        val factory = factory.getMeasureElementFactory(element) ?: return MeasureResult.SKIP
        return factory.measure(
            elements = elements,
            element = element,
            isStartElement = isStartElement,
            startOffset = startOffset,
            usedHeight = usedHeight,
            availableHeight = availableHeight
        )
    }

    /**
     * 为单个 ContentElement 创建渲染指令
     */
    private fun renderElementCommand(
        elements: List<ContentElement>,
        element: ContentElement,
        index: Int,
        startOffset: Int,
        endOffset: Int?,
        currentY: Int
    ): RenderCommand? {
        val factory = factory.getPrerenderElementFactory(element)
        return factory?.prerender(
            elements = elements,
            element = element,
            index = index,
            startOffset = startOffset,
            endOffset = endOffset,
            currentY = currentY
        )
    }
}
