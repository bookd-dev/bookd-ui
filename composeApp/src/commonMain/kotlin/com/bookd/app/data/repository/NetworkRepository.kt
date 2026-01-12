package com.bookd.app.data.repository

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import com.russhwolf.settings.string
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.content
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withTimeout

//连接状态
sealed class ConnectionState {
    data object Checking : ConnectionState()
    data class Connected(val url: String, val isInternal: Boolean) : ConnectionState()
    data object Offline : ConnectionState()
}

// 网络配置存储
class NetworkConfigRepository(private val settings: Settings) {

    var internalUrl: String by settings.string(KEY_INTERNAL_URL, "")

    var externalUrl: String by settings.string(KEY_EXTERNAL_URL, "")

    //上次错过连接的URL(启动时应该优先尝试)
    var lastConnectedUrl: String?
        get() = settings.getStringOrNull(KEY_LAST_CONNECTED)
        set(value) = value?.let { settings.putString(KEY_LAST_CONNECTED, it) }
            ?: settings.remove(KEY_LAST_CONNECTED)

    //是否配置了网络环境
    fun hasConfig() = internalUrl.isNotBlank() || externalUrl.isNotBlank()

    companion object {
        private const val KEY_INTERNAL_URL = "network_internal_url"
        private const val KEY_EXTERNAL_URL = "network_external_url"
        private const val KEY_LAST_CONNECTED = "network_last_connected"
    }
}

//网络切换器
class NetworkSwitcher(
    private val config: NetworkConfigRepository,
    private val httpClient: HttpClient
) {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Checking)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    val currentUrl: String? get() = (_connectionState.value as? ConnectionState.Connected)?.url

    //检测并选择最佳的 URL
    suspend fun selectBestUrl(): String? {
        _connectionState.value = ConnectionState.Checking

        // 1. 优先尝试上次成功的
        config.lastConnectedUrl?.let { lastUrl ->
            if (checkConnectivity(lastUrl)) {
                val isInternal = lastUrl == config.internalUrl
                _connectionState.value = ConnectionState.Connected(lastUrl, isInternal)
                return lastUrl
            }
        }

        // 2. 尝试内网
        if (config.internalUrl.isNotBlank() && checkConnectivity(config.internalUrl)) {
            config.lastConnectedUrl = config.internalUrl
            _connectionState.value = ConnectionState.Connected(config.internalUrl, isInternal = true)
            return config.internalUrl
        }

        // 3. 尝试外网
        if (config.externalUrl.isNotBlank() && checkConnectivity(config.externalUrl)) {
            config.lastConnectedUrl = config.externalUrl
            _connectionState.value = ConnectionState.Connected(config.externalUrl, isInternal = false)
            return config.externalUrl
        }

        // 4. 离线模式
        _connectionState.value = ConnectionState.Offline
        return null
    }

    // 定期检测（可选，用于自动切换）
    suspend fun startMonitor(intervalMS: Long = 30_000L) {
        while (true) {
            delay(intervalMS)
            selectBestUrl()
        }
    }

    private suspend fun checkConnectivity(url: String): Boolean {
        return try {
            withTimeout(3000L) {
                val response = httpClient.get("$url/api/health")
                response.status.isSuccess()
            }
        } catch (_: Exception) {
            false
        }
    }
}