package com.bookd.app.data.auth

/**
 * Header 提供者接口
 * 用于 Ktor 请求统一添加 Headers
 */
interface HeaderProvider {
    fun getHeaders(): Map<String, String>
}

/**
 * 默认 Header 提供者实现
 * 管理 Token 和其他通用 Headers
 */
class DefaultHeaderProvider : HeaderProvider {
    private var _token: String? = null
    
    val token: String?
        get() = _token
    
    fun updateToken(token: String?) {
        _token = token
    }
    
    fun clearToken() {
        _token = null
    }
    
    override fun getHeaders(): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        _token?.let { headers["Authorization"] = "Bearer $it" }
        return headers
    }
}
