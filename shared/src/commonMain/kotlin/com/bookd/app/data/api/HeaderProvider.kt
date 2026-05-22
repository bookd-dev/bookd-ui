package com.bookd.app.data.api

import com.bookd.app.data.repository.LanguageRepository

/**
 * Header 提供者接口
 * 用于 Ktor 请求统一添加 Headers
 */
interface HeaderProvider {
    fun getHeaders(): Map<String, String>
}

/**
 * Token 提供者接口
 * 由 UserRepository 实现
 */
interface TokenProvider {
    var token: String?
}

/**
 * 默认 Header 提供者实现
 * 
 * 自动添加以下 Headers:
 * - Authorization: Bearer token (如果已登录)
 * - Accept-Language: 用户语言偏好 (用于服务端国际化)
 */
class DefaultHeaderProvider(
    private val tokenProvider: () -> TokenProvider?,
    private val languageProvider: () -> LanguageRepository
) : HeaderProvider {
    
    override fun getHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        // Authorization header
        tokenProvider()?.token?.let { headers["Authorization"] = "Bearer $it" }
        
        // Accept-Language header (用于服务端国际化错误消息)
        headers["Accept-Language"] = languageProvider().getAcceptLanguageHeader()
        
        return headers
    }
}
