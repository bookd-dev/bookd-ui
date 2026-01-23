package com.bookd.app.basic.reader.factory

import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.data.model.ContentElement

interface ContentElementFactory <in T : ContentElement> {


    /**
     * @param element
     * @param startOffset 从第几个字符开始
     * @param availableHeight 剩余可用高度
     */
    fun measure(elements: List<ContentElement>, element: T, startOffset: Int, availableHeight: Int): MeasureResult
}