package com.bookd.app.data.api

import com.bookd.app.data.converter.SuccessResponseConverterFactory
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.HttpClient

/**
 * 网络地址提供者接口
 * 抽象当前激活的服务器地址，方便测试时注入 fake 实现
 */
interface NetworkAddressProvider {
    val currentUrl: String?
}

/**
 * API 提供者
 * 动态创建 Ktorfit 实例，处理无网络配置的情况
 */
open class ApiProvider(
    private val httpClient: HttpClient,
    private val networkSwitcher: NetworkAddressProvider
) {
    private var cachedKtorfit: Ktorfit? = null
    private var cachedBaseUrl: String? = null
    
    /**
     * 获取当前 Ktorfit 实例
     * @throws NoNetworkConfigException 如果没有配置网络地址
     */
    fun getKtorfit(): Ktorfit {
        val currentUrl = networkSwitcher.currentUrl
            ?: throw NoNetworkConfigException()
        
        // 如果 URL 变化，重新创建 Ktorfit
        if (cachedKtorfit == null || cachedBaseUrl != currentUrl) {
            cachedBaseUrl = currentUrl
            cachedKtorfit = Ktorfit.Builder()
                .baseUrl(ensureTrailingSlash(currentUrl))
                .httpClient(httpClient)
                .converterFactories(SuccessResponseConverterFactory())
                .build()
        }
        
        return cachedKtorfit!!
    }
    
    /**
     * 是否已配置网络
     */
    val isConfigured: Boolean
        get() = networkSwitcher.currentUrl != null
    
    /**
     * 获取 AuthApi，如果未配置网络返回 null
     */
    fun getAuthApiOrNull(): AuthApi? {
        return if (isConfigured) getKtorfit().createAuthApi() else null
    }
    
    /**
     * 获取 HealthApi，如果未配置网络返回 null
     */
    fun getHealthApiOrNull(): HealthApi? {
        return if (isConfigured) getKtorfit().createHealthApi() else null
    }
    
    /**
     * 获取 BookSourceApi，如果未配置网络返回 null
     */
    fun getBookSourceApiOrNull(): BookSourceApi? {
        return if (isConfigured) getKtorfit().createBookSourceApi() else null
    }
    
    /**
     * 获取 BookApi，如果未配置网络返回 null
     */
    fun getBookApiOrNull(): BookApi? {
        return if (isConfigured) getKtorfit().createBookApi() else null
    }
    
    /**
     * 获取 BookshelfApi，如果未配置网络返回 null
     */
    fun getBookshelfApiOrNull(): BookshelfApi? {
        return if (isConfigured) getKtorfit().createBookshelfApi() else null
    }
    
    /**
     * 获取 ReaderApi，如果未配置网络返回 null
     */
    open fun getReaderApiOrNull(): ReaderApi? {
        return if (isConfigured) getKtorfit().createReaderApi() else null
    }
    
    private fun ensureTrailingSlash(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
}

/**
 * 未配置网络异常
 * UI 层根据此异常类型显示对应的国际化消息
 */
class NoNetworkConfigException : Exception()
