package com.bookd.app.basic.reader.factory.internal

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import com.bookd.app.basic.reader.controller.ParagraphInlineContentCollector
import com.bookd.app.basic.reader.controller.ParagraphInlineContentInfo
import com.bookd.app.basic.reader.controller.ReaderStyleController
import com.bookd.app.data.model.ContentElement
import com.bookd.app.data.model.TextSpan

internal const val FOOTNOTE_ID_KEY = 1
internal const val FOOTNOTE_SRC_KEY = 2
internal const val FOOTNOTE_INDEX_KEY = 3

/**
 * 脚注渲染，比较特殊不会继承 [com.bookd.app.basic.reader.factory.IContentMeasureFactory]
 */
internal fun AnnotatedString.Builder.appendFootnoteInlineContent(
    styleController: ReaderStyleController,
    density: Density,
    inlineCollector: ParagraphInlineContentCollector,
    elements: List<ContentElement>,
    span: TextSpan,
): Boolean {
    if (span.footnoteId.isNullOrBlank()) return false

    /* 获取对应的脚注 */
    val footnote = elements
        .filterIsInstance<ContentElement.Footnote>()
        .firstOrNull { it.footnoteId == span.footnoteId }
        ?: return false

    return if (!footnote.footnoteImage.isNullOrBlank()) {
        appendFootnoteImageInlineContent(
            styleController = styleController,
            density = density,
            inlineCollector = inlineCollector,
            footnote = footnote
        )
        true
    } else {
        appendFootnoteTextContent(
            styleController = styleController,
            footnote = footnote
        )
    }
}

private fun AnnotatedString.Builder.appendFootnoteImageInlineContent(
    styleController: ReaderStyleController,
    density: Density,
    inlineCollector: ParagraphInlineContentCollector,
    footnote: ContentElement.Footnote,
) {
    val placeholder = buildFootnotePlaceholder(
        footnote = footnote,
        density = density,
        styleController = styleController
    )

    // 同一段可能重复引用同一脚注；key 带出现位置，避免后续引用退回原始 [n] 文本。
    val start = length
    val inlineId = "footnote:${footnote.footnoteId}:$start:${footnote.footnoteImage}"

    // 插入inlineContent占位，为什么不使用直接add是为了方便到时候替换成我需要的脚注
    // 真正插入文本占位
    appendInlineContent(
        id = inlineId,
        alternateText = "\uFFFC" // Object Replacement Character
    )
    // 文本插入后，再获取
    val end = length
    inlineCollector[inlineId] = ParagraphInlineContentInfo(
        id = inlineId,
        src = footnote.footnoteImage,
        index = 0,
        range = AnnotatedString.Range(
            start = start,
            end = end,
            item = placeholder
        )
    )
}

private fun AnnotatedString.Builder.appendFootnoteTextContent(
    styleController: ReaderStyleController,
    footnote: ContentElement.Footnote,
): Boolean {
    val textSpan = footnote.footnoteSpan ?: return false //如果是空的就不渲染了

    withStyle(
        style = styleController.buildMeasureSpanStyle(
            span = textSpan,
            style = styleController.textStyles.footnoteTextStyle
        )
    ) {
        append(textSpan.text)
    }
    return true
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
