package com.bookd.app.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.string

/**
 * 语言仓库
 * 
 * 管理应用语言设置，支持：
 * - "auto": 跟随系统语言
 * - "en": 英文
 * - "zh": 简体中文
 */
class LanguageRepository(settings: Settings) {
    
    /**
     * 用户选择的语言
     * - "auto": 跟随系统
     * - "en": 英文
     * - "zh": 简体中文
     */
    var selectedLanguage: String by settings.string(KEY_SELECTED_LANGUAGE, LANGUAGE_AUTO)
    
    /**
     * 获取当前实际使用的语言代码
     * 如果是 auto 则返回系统语言
     */
    fun getCurrentLanguage(): String {
        return if (selectedLanguage == LANGUAGE_AUTO) {
            getSystemLanguage()
        } else {
            selectedLanguage
        }
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
    
    companion object {
        const val KEY_SELECTED_LANGUAGE = "app_language"
        
        const val LANGUAGE_AUTO = "auto"
        const val LANGUAGE_EN = "en"
        const val LANGUAGE_ZH = "zh"
    }
}

/**
 * 获取系统语言
 * 返回语言代码，如 "en", "zh" 等
 */
expect fun getSystemLanguage(): String
