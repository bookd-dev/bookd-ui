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
     * @param availableHeight 剩余可用高度
     */
    fun measure(elements: List<ContentElement>, element: T, startOffset: Int, availableHeight: Int): MeasureResult
}

class ContentElementFactories(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density,
) {
    val paragraphMeasureFactory = ParagraphMeasureFactory(contentWidth, contentHeight, textMeasurer, styleController, density)


}