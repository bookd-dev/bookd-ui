package com.bookd.app.basic.reader.data

import kotlinx.serialization.Serializable

// 分页锚点：描述一页从哪里开始
@Serializable
data class PageAnchor(
    val elementIndex: Int, // 第几个 ContentElement
    val textOffset: Int,   // 如果是文本，从第几个字符开始
    val pageIndex: Int     // 页码
)