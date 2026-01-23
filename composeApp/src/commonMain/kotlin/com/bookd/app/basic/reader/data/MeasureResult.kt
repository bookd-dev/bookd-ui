package com.bookd.app.basic.reader.data

import kotlinx.serialization.Serializable

@Serializable
data class MeasureResult(
    val measuredHeight: Int,
    val isSplit: Boolean,
    val nextOffset: Int
)