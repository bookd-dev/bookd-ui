package com.bookd.app.basic.reader.data

import kotlinx.serialization.Serializable

@Serializable
data class MeasureResult(
    val measuredHeight: Int,
    val isSplit: Boolean,
    val nextOffset: Int
) {
    companion object {
        val SKIP = MeasureResult(0, false, 0)

        val NEXT = MeasureResult(0, true, 0)
    }
}