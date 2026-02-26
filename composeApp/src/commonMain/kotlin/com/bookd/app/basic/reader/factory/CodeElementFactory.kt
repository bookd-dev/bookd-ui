package com.bookd.app.basic.reader.factory

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import coil3.compose.AsyncImagePainter
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.data.MeasureResult
import com.bookd.app.basic.reader.data.RenderCommand
import com.bookd.app.basic.reader.factory.internal.shouldAddTopSpacing
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
class CodeElementFactory(
    private val contentWidth: Int,
    private val contentHeight: Int,
    private val textMeasurer: TextMeasurer,
    private val styleController: ReaderStyleController,
    private val density: Density,
) : IContentMeasureFactory<ContentElement.Code, RenderCommand.Code> {

    private val spacing = styleController.sizeStyles.getLineSpacingPx(density) / 2

    private val borderWidth = styleController.sizeStyles.getBorderWidth(density)

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

        val topSpacing = if (shouldAddTopSpacing(elements, element, usedHeight)) {
            styleController.sizeStyles.getLineSpacingPx(density)
        } else {
            0
        }

        val languageHeight = if (!element.language.isNullOrEmpty()) {
            calculateLanguageHeight(element.language)
        } else {
            0
        }

        val codeTextHeight = calculateCodeTextHeight(element.text)

        val borderWidth = borderWidth
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

    override fun prerender(
        elements: List<ContentElement>,
        element: ContentElement.Code,
        index: Int,
        startOffset: Int,
        endOffset: Int?,
        currentY: Int
    ): RenderCommand.Code {
        var y = currentY

        if (shouldAddTopSpacing(elements, element, y)) {
            y += styleController.sizeStyles.getLineSpacingPx(density)
        }

        val languageLayout = if (!element.language.isNullOrEmpty()) {
            textMeasurer.measure(
                text = element.language.uppercase(),
                style = styleController.textStyles.codeTextStyle,
                constraints = Constraints(maxWidth = contentWidth - spacing * 2)
            )
        } else {
            null
        }

        val codeTextLayout = textMeasurer.measure(
            text = element.text,
            style = styleController.textStyles.codeTextStyle,
            constraints = Constraints(maxWidth = contentWidth - spacing * 2)
        )

        val languageSpacing = if (languageLayout != null) spacing else 0
        val verticalPadding = spacing * 2
        val totalHeight = borderWidth + verticalPadding +
            (languageLayout?.size?.height ?: 0) + languageSpacing +
            codeTextLayout.size.height

        return RenderCommand.Code(
            y = y,
            textLayout = codeTextLayout,
            languageLayout = languageLayout,
            height = totalHeight
        )
    }

    override fun draw(
        drawScope: DrawScope,
        imagePainters: Map<String, AsyncImagePainter>,
        command: RenderCommand.Code
    ) {
        val containerTop = command.y.toFloat()

        drawScope.drawRect(
            color = styleController.colorStyles.codeBlockBackground,
            topLeft = Offset(0f, containerTop),
            size = Size(contentWidth.toFloat(), command.height.toFloat())
        )
        drawScope.drawRect(
            color = styleController.colorStyles.codeBlockBorder,
            topLeft = Offset(0f, containerTop),
            size = Size(contentWidth.toFloat(), command.height.toFloat()),
            style = Stroke(width = borderWidth.toFloat())
        )

        var textTop = containerTop + spacing
        if (command.languageLayout != null) {
            drawScope.drawText(
                textLayoutResult = command.languageLayout,
                topLeft = Offset(spacing.toFloat(), textTop)
            )
            textTop += command.languageLayout.size.height + spacing
        }

        drawScope.drawText(
            textLayoutResult = command.textLayout,
            topLeft = Offset(spacing.toFloat(), textTop)
        )
    }

    private fun calculateLanguageHeight(language: String): Int {
        val textLayoutResult = textMeasurer.measure(
            text = language,
            style = styleController.textStyles.codeTextStyle,
            constraints = Constraints(maxWidth = contentWidth - spacing * 2)
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
