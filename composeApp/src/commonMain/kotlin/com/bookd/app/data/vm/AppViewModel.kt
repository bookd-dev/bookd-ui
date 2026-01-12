package com.bookd.app.data.vm

import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.repository.NetworkSwitcher
import com.bookd.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


class AppViewModel(
    private val networkSwitcher: NetworkSwitcher,
    private val userRepository: UserRepository,
    private val apiProvider: ApiProvider,
) : BaseViewModel(true) {

    val connectionState = networkSwitcher.connectionState
    val authState = userRepository.authState

    // 初始化状态
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()
    
    private val _error = MutableStateFlow<Result<Any>?>(null)
    val error = _error.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        scope.launch {
            // 1. 检测网络
            networkSwitcher.selectBestUrl()
            // 2. 恢复登录状态
            userRepository.initialize()
            // 3. 标记初始化完成
            _isInitialized.value = true
        }
    }

    // 手动重试网络
    fun retryNetwork() {
        scope.launch {
            networkSwitcher.selectBestUrl()
            // 网络配置后重新初始化用户状态
            if (apiProvider.isConfigured) {
                userRepository.initialize()
            }
        }
    }

    // 开启后台网络监控（可选）
    fun startNetworkMonitor() {
        scope.launch {
            networkSwitcher.startMonitor()
        }
    }


    override fun handleException(context: CoroutineContext, exception: Throwable) {
        _error.value = Result.failure(exception)
    }
}
