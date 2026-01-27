package com.bookd.app.basic.reader.factory

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.factory.internal.shouldAddTopSpacing
import com.bookd.app.data.model.ContentElement

class DividerElementFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density
) : IContentMeasureFactory<ContentElement.Divider, RenderCommand.Divider> {

    private val spacing = styleController.sizeStyles.getLineSpacingPx(density)

    private val borderWidth = styleController.sizeStyles.getBorderWidth(density)

    override fun measure(
        elements: List<ContentElement>,
        element: ContentElement.Divider,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int
    ): MeasureResult {
        if (startOffset > 0) {
            return MeasureResult.NEXT
        }

        val totalHeight = if (shouldAddTopSpacing(elements, element, usedHeight)) spacing + borderWidth else borderWidth

        return if (totalHeight <= availableHeight) {
            MeasureResult(totalHeight, false, 0)
        } else {
            MeasureResult.NEXT
        }
    }
}