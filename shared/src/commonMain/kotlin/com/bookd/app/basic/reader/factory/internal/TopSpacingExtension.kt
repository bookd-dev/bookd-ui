package com.bookd.app.basic.reader.factory.internal

import com.bookd.app.data.model.ContentElement

/**
 * 是否应该添加段落间隔
 */
internal fun shouldAddTopSpacing(
    elements: List<ContentElement>,
    element: ContentElement,
    usedHeight: Int
): Boolean {
    val elementIndex = elements.indexOf(element)

    // 1. 页面第一行（currentY == 0）不添加（避免顶部间距）
    if (usedHeight == 0) return false

    // 2 标题后面时，通常不需要额外的间距
    if (elementIndex > 0 && elements[elementIndex - 1] is ContentElement.Heading) {
        return false
    }

    return true
}