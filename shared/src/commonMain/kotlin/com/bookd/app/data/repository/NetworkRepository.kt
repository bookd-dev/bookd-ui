package com.bookd.app.data.repository

import com.bookd.app.data.api.NetworkAddressProvider
import com.bookd.app.data.structure.UrlTestState
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

/**
 * 连接状态（表示当前的连接情况）
 */
sealed class ConnectionStatus {
    data object Checking : ConnectionStatus()
    data class Connected(val url: String, val isInternal: Boolean) : ConnectionStatus()
    data object Offline : ConnectionStatus()
}

/**
 * 网络状态（组合连接状态和各 URL 的测试结果）
 */
data class NetworkState(
    val status: ConnectionStatus = ConnectionStatus.Checking,
    val internalUrlState: UrlTestState = UrlTestState.Pending,
    val externalUrlState: UrlTestState = UrlTestState.Pending
)

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
) : NetworkAddressProvider {
    private val _networkState = MutableStateFlow(NetworkState())
    val networkState: StateFlow<NetworkState> = _networkState

    override val currentUrl: String?
        get() = (_networkState.value.status as? ConnectionStatus.Connected)?.url

    //检测并选择最佳的 URL
    suspend fun selectBestUrl(): String? {
        // 开始检测，重置状态
        _networkState.value = NetworkState(
            status = ConnectionStatus.Checking,
            internalUrlState = if (config.internalUrl.isNotBlank()) UrlTestState.Pending else UrlTestState.Pending,
            externalUrlState = if (config.externalUrl.isNotBlank()) UrlTestState.Pending else UrlTestState.Pending
        )

        // 1. 优先尝试上次成功的
        config.lastConnectedUrl?.let { lastUrl ->
            val success = checkConnectivity(lastUrl)
            updateUrlState(lastUrl, success)
            if (success) {
                val isInternal = lastUrl == config.internalUrl
                _networkState.value = _networkState.value.copy(
                    status = ConnectionStatus.Connected(lastUrl, isInternal)
                )
                // 测试另一个 URL（后台）
                testOtherUrl(lastUrl)
                return lastUrl
            }
        }

        // 2. 尝试内网
        if (config.internalUrl.isNotBlank()) {
            val success = checkConnectivity(config.internalUrl)
            _networkState.value = _networkState.value.copy(
                internalUrlState = if (success) UrlTestState.Success else UrlTestState.Failed
            )
            if (success) {
                config.lastConnectedUrl = config.internalUrl
                _networkState.value = _networkState.value.copy(
                    status = ConnectionStatus.Connected(config.internalUrl, isInternal = true)
                )
                // 测试外网（后台）
                testExternalUrl()
                return config.internalUrl
            }
        }

        // 3. 尝试外网
        if (config.externalUrl.isNotBlank()) {
            val success = checkConnectivity(config.externalUrl)
            _networkState.value = _networkState.value.copy(
                externalUrlState = if (success) UrlTestState.Success else UrlTestState.Failed
            )
            if (success) {
                config.lastConnectedUrl = config.externalUrl
                _networkState.value = _networkState.value.copy(
                    status = ConnectionStatus.Connected(config.externalUrl, isInternal = false)
                )
                return config.externalUrl
            }
        }

        // 4. 离线模式
        _networkState.value = _networkState.value.copy(
            status = ConnectionStatus.Offline
        )
        return null
    }

    // 更新指定 URL 的测试状态
    private fun updateUrlState(url: String, success: Boolean) {
        val state = if (success) UrlTestState.Success else UrlTestState.Failed
        when (url) {
            config.internalUrl -> _networkState.value = _networkState.value.copy(internalUrlState = state)
            config.externalUrl -> _networkState.value = _networkState.value.copy(externalUrlState = state)
        }
    }

    // 测试另一个 URL（当一个已成功连接时）
    private suspend fun testOtherUrl(connectedUrl: String) {
        when (connectedUrl) {
            config.internalUrl -> testExternalUrl()
            config.externalUrl -> testInternalUrl()
        }
    }

    private suspend fun testInternalUrl() {
        if (config.internalUrl.isNotBlank()) {
            val success = checkConnectivity(config.internalUrl)
            _networkState.value = _networkState.value.copy(
                internalUrlState = if (success) UrlTestState.Success else UrlTestState.Failed
            )
        }
    }

    private suspend fun testExternalUrl() {
        if (config.externalUrl.isNotBlank()) {
            val success = checkConnectivity(config.externalUrl)
            _networkState.value = _networkState.value.copy(
                externalUrlState = if (success) UrlTestState.Success else UrlTestState.Failed
            )
        }
    }

    // 定期检测（可选，用于自动切换）
    suspend fun startMonitor(intervalMS: Long = 30_000L) {
        while (config.hasConfig()) {
            delay(intervalMS)
            selectBestUrl()
        }
    }

    /**
     * 测试单个 URL 的连通性
     */
    suspend fun testUrl(url: String): UrlTestState {
        if (url.isBlank()) return UrlTestState.Pending
        return if (checkConnectivity(url)) {
            UrlTestState.Success
        } else {
            UrlTestState.Failed
        }
    }

    private suspend fun checkConnectivity(url: String): Boolean {
        return try {
            withTimeout(5000L) {
                val response = httpClient.get("$url/api/health")
                response.status.isSuccess()
            }
        } catch (_: Exception) {
            false
        }
    }
}
