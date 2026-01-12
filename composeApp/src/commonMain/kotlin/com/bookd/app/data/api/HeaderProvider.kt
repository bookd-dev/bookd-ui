package com.bookd.app.data.api

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
 * 从 TokenProvider 读取 token，不负责存储
 */
class DefaultHeaderProvider(
    private val tokenProvider: () -> TokenProvider?
) : HeaderProvider {
    
    override fun getHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        tokenProvider()?.token?.let { headers["Authorization"] = "Bearer $it" }
        return headers
    }
}
