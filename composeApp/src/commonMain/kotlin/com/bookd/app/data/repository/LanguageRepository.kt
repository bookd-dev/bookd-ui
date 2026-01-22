package com.bookd.app.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.string

/**
 * 语言仓库
 * 
 * 管理应用语言设置（目前仅用于 HTTP Accept-Language header）
 */
class LanguageRepository(settings: Settings) {
    
    /**
     * 获取当前实际使用的语言代码
     */
    fun getCurrentLanguage(): String {
        return getSystemLanguage()
    }
    
    /**
     * 获取 HTTP Accept-Language header 值
     */
    fun getAcceptLanguageHeader(): String {
        return when (getCurrentLanguage()) {
            "zh" -> "zh-CN,zh;q=0.9,en;q=0.8"
            else -> "en-US,en;q=0.9"
        }
    }
}

/**
 * 获取系统语言
 * 返回语言代码，如 "en", "zh" 等
 */
expect fun getSystemLanguage(): String
