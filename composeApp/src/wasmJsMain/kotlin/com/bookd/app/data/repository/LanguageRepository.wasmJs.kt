package com.bookd.app.data.repository

import kotlinx.browser.window

/**
 * WasmJs (Web) 平台获取系统语言
 */
actual fun getSystemLanguage(): String {
    val language = window.navigator.language
    // navigator.language 返回如 "zh-CN", "en-US"，取前两个字符作为语言代码
    return language.take(2).lowercase()
}
