package com.bookd.app.data.vm

import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.basic.extension.logE
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.repository.NetworkSwitcher
import com.bookd.app.data.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

internal class AppErrorEventQueue {
    private val channel = Channel<Throwable>(Channel.BUFFERED)

    val events = channel.receiveAsFlow()

    fun send(exception: Throwable) = channel.trySend(exception)
}


class AppViewModel(
    private val networkSwitcher: NetworkSwitcher,
    private val userRepository: UserRepository,
    private val apiProvider: ApiProvider,
) : BaseViewModel(true) {

    val networkState = networkSwitcher.networkState
    val authState = userRepository.authState

    // 初始化状态
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized = _isInitialized.asStateFlow()
    
    // 错误属于一次性 UI 事件：缓冲等待 UI 收集，但消费后不因页面重建而重放。
    private val errorEventQueue = AppErrorEventQueue()
    val errorEvents = errorEventQueue.events

    init {
        initialize()
        startNetworkMonitor()
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

    fun startNetworkMonitor() {
        scope.launch {
            networkSwitcher.startMonitor()
        }
    }


    override fun handleException(context: CoroutineContext, exception: Throwable) {
        val result = errorEventQueue.send(exception)
        if (result.isFailure) {
            logE(tag = "AppViewModel") { "全局错误事件发送失败: $exception" }
        }
    }
}
