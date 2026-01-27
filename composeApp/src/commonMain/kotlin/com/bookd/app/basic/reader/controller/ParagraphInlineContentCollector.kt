package com.bookd.app.basic.reader.controller

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder

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
    private val inlineContentPlaceholders = mutableMapOf<String, AnnotatedString.Range<Placeholder>>()


    operator fun get(inlineId: String): AnnotatedString.Range<Placeholder>? {
        return inlineContentPlaceholders[inlineId]
    }

    operator fun contains(inlineId: String): Boolean {
        return inlineContentPlaceholders.containsKey(inlineId)
    }

    operator fun set(inlineId: String, placeholder: AnnotatedString.Range<Placeholder>) {
        inlineContentPlaceholders[inlineId] = placeholder
    }

    operator fun set(inlineId: String, start: Int, end: Int, placeholder: Placeholder) {
        val range = AnnotatedString.Range(start = start, end = end, item = placeholder)
        inlineContentPlaceholders[inlineId] = range
    }

    fun clear() {
        inlineContentPlaceholders.clear()
    }

    fun getAdjustPlaceholder(text: AnnotatedString, startOffset: Int): List<AnnotatedString.Range<Placeholder>> {
        val placeholders = inlineContentPlaceholders.values.toList()
        if (placeholders.isEmpty()) return emptyList()

        // 【关键步骤】计算适配当前 textToMeasure 的 placeholders
        return if (startOffset == 0) {
            placeholders
        } else {
            // 如果截取了字符串，需要：
            // 1. 过滤掉不在当前截取范围内的占位符
            // 2. 将占位符的 start/end 减去 startOffset
            placeholders.mapNotNull { range ->
                if (range.start >= startOffset && range.end <= text.length) {
                    AnnotatedString.Range(
                        item = range.item,
                        start = range.start - startOffset, // 平移坐标
                        end = range.end - startOffset      // 平移坐标
                    )
                } else {
                    null // 过滤掉已经被切掉的占位符
                }
            }
        }
    }
}