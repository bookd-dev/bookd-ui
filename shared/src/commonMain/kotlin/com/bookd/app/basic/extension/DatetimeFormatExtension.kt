package com.bookd.app.basic.extension

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

private val DEFAULT_HOUR_MINUTE = LocalDateTime.Format {
    hour()
    char(':')
    minute()
}

/**
 * 获取当前时间字符串，默认（HH:mm 格式）
 */
fun getCurrentTimeString(
    formatter: DateTimeFormat<LocalDateTime> = DEFAULT_HOUR_MINUTE,
): String {
    val now = Clock.System.now()
    val timezone = TimeZone.currentSystemDefault()
    return now.toLocalDateTime(timezone).format(formatter)
}
