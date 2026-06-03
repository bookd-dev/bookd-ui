package com.bookd.app.basic.reader.factory

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import coil3.compose.AsyncImagePainter
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.data.model.ContentElement

interface IContentMeasureFactory <in T : ContentElement, R : RenderCommand> {


    /**
     * 测量
     * @param element
     * @param startOffset 从第几个字符开始
     * @param usedHeight 已经使用的高度
     * @param availableHeight 剩余可用高度
     */
    fun measure(
        elements: List<ContentElement>,
        element: T,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int,
    ): MeasureResult

    /**
     * 预渲染
     *
     * @param element
     * @param index 渲染的index
     * @param startOffset
     */
    fun prerender(
        elements: List<ContentElement>,
        element: T,
        index: Int,
        startOffset: Int,
        endOffset: Int?,
        currentY: Int
    ): R

    /**
     * 绘制
     */
    fun draw(
        drawScope: DrawScope,
        imagePainters: Map<String, AsyncImagePainter>,
        command: R,
    )
}

class ContentElementFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density,
) {
    /**
     * 标题测量工厂
     */
    val headlineElementFactory = HeadlineElementFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 段落测量工厂
     */
    val paragraphElementFactory = ParagraphElementFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 图片测量工厂
     */
    val imageElementFactory = ImageElementFactory(contentWidth, contentHeight, textMeasurer, styleController, density,)

    /**
     * 代码测量工厂
     */
    val codeElementFactory = CodeElementFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 分隔线测量工厂
     */
    val dividerElementFactory = DividerElementFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 列表块工厂
     */
    val listBlockElementFactory = ListBlockElementFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 引用工厂
     */
    val quoteElementFactory = QuoteElementFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    @Suppress("UNCHECKED_CAST")
    fun getMeasureElementFactory(element: ContentElement): IContentMeasureFactory<ContentElement, RenderCommand>? {
        return when(element) {
            is ContentElement.Heading -> headlineElementFactory
            is ContentElement.Paragraph -> paragraphElementFactory
            is ContentElement.Image -> imageElementFactory
            is ContentElement.Footnote -> null //不参与绘制测量，因为在段落内处理了
            is ContentElement.Code -> codeElementFactory
            is ContentElement.Divider -> dividerElementFactory
            is ContentElement.ListBlock -> listBlockElementFactory
            is ContentElement.Quote -> quoteElementFactory
        } as? IContentMeasureFactory<ContentElement, RenderCommand>
    }

    @Suppress("UNCHECKED_CAST")
    fun getPrerenderElementFactory(element: ContentElement): IContentMeasureFactory<ContentElement, RenderCommand>? {
        return when(element) {
            is ContentElement.Heading -> headlineElementFactory
            is ContentElement.Paragraph -> paragraphElementFactory
            is ContentElement.Image -> imageElementFactory
            is ContentElement.Footnote -> null //不参与绘制测量，因为在段落内处理了
            is ContentElement.Code -> codeElementFactory
            is ContentElement.Divider -> dividerElementFactory
            is ContentElement.ListBlock -> listBlockElementFactory
            is ContentElement.Quote -> quoteElementFactory
        } as? IContentMeasureFactory<ContentElement, RenderCommand>
    }

    @Suppress("UNCHECKED_CAST")
    fun getDrawElementFactory(command: RenderCommand): IContentMeasureFactory<ContentElement, RenderCommand>? {
        return when(command) {
            is RenderCommand.Heading -> headlineElementFactory
            is RenderCommand.Text -> paragraphElementFactory
            is RenderCommand.Image -> imageElementFactory
            is RenderCommand.Footnote -> null //不参与绘制测量，因为在段落内处理了
            is RenderCommand.Code -> codeElementFactory
            is RenderCommand.Divider -> dividerElementFactory
            is RenderCommand.ListItem -> listBlockElementFactory
            is RenderCommand.ListBlock -> listBlockElementFactory
            is RenderCommand.Quote -> quoteElementFactory
        } as? IContentMeasureFactory<ContentElement, RenderCommand>
    }
}
