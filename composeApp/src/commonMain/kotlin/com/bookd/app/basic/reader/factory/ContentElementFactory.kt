package com.bookd.app.basic.reader.factory

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.data.model.ContentElement

interface IContentElementFactory <in T : ContentElement> {


    /**
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
    val headlineMeasureFactory = HeadlineMeasureFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 段落测量工厂
     */
    val paragraphMeasureFactory = ParagraphMeasureFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 图片测量工厂
     */
    val imageMeasureFactory = ImageMeasureFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 代码测量工厂
     */
    val codeMeasureFactory = CodeMeasureFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 分隔线测量工厂
     */
    val dividerMeasureFactory = DividerMeasureFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 列表块工厂
     */
    val listDividerMeasureFactory = ListBlockMeasureFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    /**
     * 引用工厂
     */
    val quoteMeasureFactory = QuoteMeasureFactory(contentWidth, contentHeight, textMeasurer, styleController, density)

    @Suppress("UNCHECKED_CAST")
    fun getMeasureElementFactory(element: ContentElement): IContentElementFactory<ContentElement>? {
        return when(element) {
            is ContentElement.Heading -> headlineMeasureFactory
            is ContentElement.Paragraph -> paragraphMeasureFactory
            is ContentElement.Image -> imageMeasureFactory
            is ContentElement.Footnote -> null //不参与绘制测量，因为在段落内处理了
            is ContentElement.Code -> codeMeasureFactory
            ContentElement.Divider -> dividerMeasureFactory
            is ContentElement.ListBlock -> listDividerMeasureFactory
            is ContentElement.Quote -> quoteMeasureFactory
        } as? IContentElementFactory<ContentElement>
    }
}