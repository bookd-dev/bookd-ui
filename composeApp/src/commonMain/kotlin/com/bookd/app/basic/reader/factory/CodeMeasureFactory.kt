package com.bookd.app.basic.reader.factory

import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.data.model.ContentElement

/**
 * code 样式
 *
 * ----------------------------------
 * ｜Kotlin                          ｜
 * ｜                                ｜
 * ｜class Test {                    ｜
 * ｜    val temp = ""               ｜
 * ｜}                               ｜
 * ｜________________________________｜
 */
class CodeMeasureFactor(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density,
) : IContentElementFactory<ContentElement.Code> {

    private val spacing = styleController.spacingStyles.getLineSpacingPx(density) / 2

    private val borderWidth = styleController.spacingStyles.getBorderWidth(density)

    override fun measure(
        elements: List<ContentElement>,
        element: ContentElement.Code,
        isStartElement: Boolean,
        startOffset: Int,
        usedHeight: Int,
        availableHeight: Int
    ): MeasureResult {
        // 代码块不支持分页，必须完整显示
        if (startOffset > 0) {
            return MeasureResult.NEXT
        }

        val topSpacing = if (shouldAddTopSpacing(usedHeight)) {
            styleController.spacingStyles.getLineSpacingPx(density)
        } else {
            0
        }

        val languageHeight = if (!element.language.isNullOrEmpty()) {
            calculateLanguageHeight(element.language)
        } else {
            0
        }

        val codeTextHeight = calculateCodeTextHeight(element.text)

        val borderWidth = 2
        val languageSpacing = if (!element.language.isNullOrEmpty()) spacing else 0
        val verticalPadding = spacing * 2

        val totalHeight = topSpacing + borderWidth + languageHeight + languageSpacing + verticalPadding + codeTextHeight

        return if (totalHeight <= availableHeight) {
            MeasureResult(
                measuredHeight = totalHeight,
                isSplit = false,
                nextOffset = 0
            )
        } else {
            MeasureResult.NEXT
        }
    }

    private fun shouldAddTopSpacing(usedHeight: Int): Boolean {
        return usedHeight > 0
    }

    private fun calculateLanguageHeight(language: String): Int {
        val textLayoutResult = textMeasurer.measure(
            text = language,
            style = styleController.textStyles.codeTextStyle,
            constraints = Constraints(
                maxWidth = contentWidth
            )
        )
        return textLayoutResult.size.height
    }

    private fun calculateCodeTextHeight(code: String): Int {
        val textLayoutResult = textMeasurer.measure(
            text = code,
            style = styleController.textStyles.codeTextStyle,
            constraints = Constraints(
                maxWidth = contentWidth - spacing * 2
            )
        )
        return textLayoutResult.size.height
    }
}