package com.bookd.app.basic.reader.factory.internal

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ParagraphInlineContentCollector
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.basic.reader.ui.FootnoteImage
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.TextSpan

/**
 * 脚注渲染，比较特殊不会继承 [com.bookd.app.basic.reader.factory.IContentElementFactory]
 */
internal fun AnnotatedString.Builder.autoAppendFootnoteInlineContent(
    styleController: ReaderStyleController,
    density: Density,
    inlineCollector: ParagraphInlineContentCollector,
    elements: List<ContentElement>,
    span: TextSpan,
) {
    if (span.footnoteId.isNullOrBlank()) return /* 不参与文本占位计算 */

    /* 获取对应的脚注 */
    val footnotes: List<ContentElement.Footnote> = elements
        .filterIsInstance<ContentElement.Footnote>()
        .filter { it.footnoteId == span.footnoteId }

    if (footnotes.isEmpty()) return /* 不参与文本占位计算 */

    footnotes.forEachIndexed { index, footnote ->
        val image = footnote.footnoteImage

        if (image != null) {
            //脚注有图片
            appendFootnoteImageInlineContent(
                styleController = styleController,
                density = density,
                inlineCollector = inlineCollector,
                index = index,
                footnote = footnote
            )
        } else {
            //脚注没有图片，使用脚注文本处理
            appendFootnoteTextContent(
                styleController = styleController,
                footnote = footnote
            )
        }
    }
}

private fun AnnotatedString.Builder.appendFootnoteImageInlineContent(
    styleController: ReaderStyleController,
    density: Density,
    inlineCollector: ParagraphInlineContentCollector,
    index: Int,
    footnote: ContentElement.Footnote,
) {

    // InlineContent 的唯一 key（必须稳定）
    val inlineId = "footnote:${footnote.footnoteId}:${index}"

    // 避免重复注册（同一页多次引用）
    if (!inlineCollector.inlineContents.containsKey(inlineId)) {

        val placeholder = buildFootnotePlaceholder(
            footnote = footnote,
            density = density,
            styleController = styleController
        )

        inlineCollector.inlineContents[inlineId] = InlineTextContent(placeholder) {
            FootnoteImage(footnote = footnote)
        }
    }

    // 真正插入文本占位
    appendInlineContent(
        id = inlineId,
        alternateText = "\uFFFC" // Object Replacement Character
    )
}

private fun AnnotatedString.Builder.appendFootnoteTextContent(
    styleController: ReaderStyleController,
    footnote: ContentElement.Footnote,
) {
    val textSpan = footnote.footnoteSpan ?: return //如果是空的就不渲染了

    withStyle(
        style = styleController.buildMeasureSpanStyle(
            span = footnote.footnoteSpan,
            style = styleController.textStyles.footnoteTextStyle
        )
    ) {
        append(textSpan.text)
    }
}

private fun buildFootnotePlaceholder(
    footnote: ContentElement.Footnote,
    styleController: ReaderStyleController,
    density: Density,
): Placeholder {

    val lineHeightPx = with(density) {
        styleController.textStyles.footnoteTextStyle.lineHeight.toPx()
    }

    val (widthPx: Float, heightPx: Float) = when {
        // 已知图片真实尺寸
        footnote.width != null && footnote.height != null -> {
            val imgWidth = footnote.width.toFloat()
            val imgHeight = footnote.height.toFloat()

            // 超过行高时按比例缩放
            if (imgHeight > lineHeightPx) {
                val scale = lineHeightPx / imgHeight
                imgWidth * scale to imgHeight * scale
            } else {
                imgWidth to imgHeight
            }
        }

        // 只知道 aspectRatio
        footnote.aspectRatio != null && footnote.aspectRatio > 0 -> {
            val w = lineHeightPx * footnote.aspectRatio.toFloat()
            w to lineHeightPx
        }

        // 都没信息，使用宽高 1:1
        else -> {
            lineHeightPx to lineHeightPx
        }
    }

    return Placeholder(
        width = with(density) { widthPx.toSp() },
        height = with(density) { heightPx.toSp() },
        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
    )
}
