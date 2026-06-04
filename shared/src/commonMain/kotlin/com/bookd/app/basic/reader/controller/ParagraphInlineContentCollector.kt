package com.bookd.app.basic.reader.controller

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.unit.IntSize
import com.bookd.app.basic.reader.data.RenderInlineContentInfo

data class ParagraphInlineContentInfo(
    val id: String,
    val src: String?,
    val index: Int,
    val range: AnnotatedString.Range<Placeholder>
)

/**
 * 段落级InlineContentCollector, 基本只有注脚用到了
 */
class ParagraphInlineContentCollector {

    /**
     * 构建placeholder，为什么这么做是因为 [androidx.compose.ui.text.TextMeasurer] 不支持 [appendInlineContent]
     *
     * 可以详细查看 [appendInlineContent] 它的注释，它详细描述了它是个 [androidx.compose.foundation.text.BasicText] 才支持的功能，
     * 所以在底层 Skia 去测量的时候，它不会生效
     */
    private val inlineContentPlaceholders = mutableMapOf<String, ParagraphInlineContentInfo>()

    operator fun contains(inlineId: String): Boolean {
        return inlineContentPlaceholders.containsKey(inlineId)
    }

    operator fun set(inlineId: String, info: ParagraphInlineContentInfo) {
        inlineContentPlaceholders[inlineId] = info
    }

    fun getAdjustPlaceholder(text: AnnotatedString, startOffset: Int): List<AnnotatedString.Range<Placeholder>> {
        return getAdjustedInlineContent(text, startOffset).map { it.range }
    }

    fun getAllInlineContent(text: AnnotatedString, startOffset: Int): Map<String, RenderInlineContentInfo>? {
        val placeholders = getAdjustedInlineContent(text, startOffset)
        if (placeholders.isEmpty()) return null

        return placeholders.withIndex().associate { (index, info) ->
            info.id to RenderInlineContentInfo(
                id = info.id,
                placeholder = info.range.item,
                src = info.src,
                index = index
            )
        }
    }

    private fun getAdjustedInlineContent(
        text: AnnotatedString,
        startOffset: Int
    ): List<ParagraphInlineContentInfo> {
        val placeholders = inlineContentPlaceholders.values.toList()
        if (placeholders.isEmpty()) return emptyList()

        return placeholders.mapNotNull { info ->
            if (info.range.start < startOffset || info.range.end > text.length) return@mapNotNull null
            info.copy(
                range = AnnotatedString.Range(
                    item = info.range.item,
                    start = info.range.start - startOffset,
                    end = info.range.end - startOffset
                )
            )
        }
    }
}
