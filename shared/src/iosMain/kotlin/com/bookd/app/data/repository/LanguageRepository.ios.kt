package com.bookd.app.data.repository

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

/**
 * iOS 平台获取系统语言
 */
actual fun getSystemLanguage(): String {
    return NSLocale.currentLocale.languageCode.takeIf { it.isNotBlank() } ?: "en"
}
