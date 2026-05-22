package com.bookd.app.data.repository

import java.util.Locale

/**
 * Android 平台获取系统语言
 */
actual fun getSystemLanguage(): String {
    return Locale.getDefault().language
}
