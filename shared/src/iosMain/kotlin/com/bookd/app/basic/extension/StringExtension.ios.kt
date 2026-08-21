package com.bookd.app.basic.extension

import kotlin.math.round

/**
 * iOS 实现的字符串格式化
 * 由于 NSString.stringWithFormat 是 C 可变参数函数，Kotlin/Native 无法直接传递数组
 * 因此使用手动替换的方式实现基本的格式化功能
 */
actual fun String.format(vararg data: Any?): String {
    var index = 0
    val regex = Regex("%[\\d.]*[sdfSDF%]|%[sdfSDF%]")

    return regex.replace(this) { matchResult ->
        val format = matchResult.value
        if (format == "%%") {
            "%"
        } else if (index < data.size) {
            val value = data[index++]
            when {
                format.endsWith("s", ignoreCase = true) -> value?.toString() ?: "null"
                format.endsWith("d", ignoreCase = true) -> (value as? Number)?.toLong()?.toString() ?: value?.toString() ?: "0"
                format.endsWith("f", ignoreCase = true) -> {
                    // 处理浮点数格式化，如 %.1f
                    val precision = Regex("\\.(\\d+)").find(format)?.groupValues?.get(1)?.toIntOrNull()
                    val number = (value as? Number)?.toDouble() ?: 0.0
                    if (precision != null) {
                        number.formatDecimal(precision)
                    } else {
                        number.toString()
                    }
                }
                else -> value?.toString() ?: "null"
            }
        } else {
            matchResult.value
        }
    }
}

/**
 * 格式化小数位数
 */
private fun Double.formatDecimal(precision: Int): String {
    var factor = 1.0
    repeat(precision) { factor *= 10.0 }
    val rounded = round(this * factor) / factor
    val str = rounded.toString()
    val dotIndex = str.indexOf('.')
    return if (dotIndex == -1) {
        str + "." + "0".repeat(precision)
    } else {
        val decimals = str.length - dotIndex - 1
        if (decimals < precision) {
            str + "0".repeat(precision - decimals)
        } else if (decimals > precision) {
            str.substring(0, dotIndex + precision + 1)
        } else {
            str
        }
    }
}