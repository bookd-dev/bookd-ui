package com.bookd.app.basic.extension

actual fun String.format(vararg data: Any?): String {
    return String.format(this, data)
}