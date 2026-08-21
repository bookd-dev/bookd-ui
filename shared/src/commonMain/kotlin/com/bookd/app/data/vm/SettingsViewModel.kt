package com.bookd.app.data.vm

import androidx.compose.runtime.Immutable
import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.data.repository.CacheRepository
import com.bookd.app.data.repository.UserRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ============= MVI State =============

/**
 * 设置页面状态
 */
@Immutable
data class SettingsState(
    /** 缓存大小（字节） */
    val cacheSize: Long = 0L,
    
    /** 是否正在计算缓存大小 */
    val isLoadingCacheSize: Boolean = true,
    
    /** 是否正在清除缓存 */
    val isClearingCache: Boolean = false,
    
    /** 是否显示清除缓存确认对话框 */
    val showClearCacheDialog: Boolean = false,
    
    /** 是否显示退出登录确认对话框 */
    val showLogoutDialog: Boolean = false,
)

// ============= MVI Intent =============

/**
 * 用户意图（用户操作）
 */
sealed interface SettingsIntent {
    /** 加载缓存大小 */
    data object LoadCacheSize : SettingsIntent
    
    /** 点击清除缓存 */
    data object ClickClearCache : SettingsIntent
    
    /** 确认清除缓存 */
    data object ConfirmClearCache : SettingsIntent
    
    /** 关闭清除缓存对话框 */
    data object DismissClearCacheDialog : SettingsIntent
    
    /** 点击网络设置 */
    data object ClickNetworkConfig : SettingsIntent
    
    /** 点击退出登录 */
    data object ClickLogout : SettingsIntent
    
    /** 确认退出登录 */
    data object ConfirmLogout : SettingsIntent
    
    /** 关闭退出登录对话框 */
    data object DismissLogoutDialog : SettingsIntent
}

// ============= MVI Effect =============

/**
 * 副作用（一次性事件）
 */
sealed interface SettingsEffect {
    /** 导航到网络配置页面 */
    data object NavigateToNetworkConfig : SettingsEffect
    
    /** 导航到登录页面（退出登录后） */
    data object NavigateToLogin : SettingsEffect
    
    /** 缓存清除成功 */
    data object ClearCacheSuccess : SettingsEffect
    
    /** 退出登录成功 */
    data object LogoutSuccess : SettingsEffect
}

// ============= ViewModel =============

class SettingsViewModel(
    private val cacheRepository: CacheRepository,
    private val userRepository: UserRepository,
) : BaseViewModel() {
    
    // State
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()
    
    // Effect（一次性事件）
    private val _effect = Channel<SettingsEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()
    
    init {
        loadCacheSize()
    }
    
    /**
     * 处理用户意图
     */
    fun onIntent(intent: SettingsIntent) {
        when (intent) {
            is SettingsIntent.LoadCacheSize -> loadCacheSize()
            is SettingsIntent.ClickClearCache -> showClearCacheDialog()
            is SettingsIntent.ConfirmClearCache -> clearCache()
            is SettingsIntent.DismissClearCacheDialog -> dismissClearCacheDialog()
            is SettingsIntent.ClickNetworkConfig -> navigateToNetworkConfig()
            is SettingsIntent.ClickLogout -> showLogoutDialog()
            is SettingsIntent.ConfirmLogout -> logout()
            is SettingsIntent.DismissLogoutDialog -> dismissLogoutDialog()
        }
    }
    
    // ============= 缓存管理 =============
    
    private fun loadCacheSize() {
        scope.launch {
            _state.update { it.copy(isLoadingCacheSize = true) }
            try {
                val size = cacheRepository.getCacheSize()
                _state.update { 
                    it.copy(cacheSize = size, isLoadingCacheSize = false) 
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoadingCacheSize = false) }
                throw e
            }
        }
    }
    
    private fun showClearCacheDialog() {
        _state.update { it.copy(showClearCacheDialog = true) }
    }
    
    private fun dismissClearCacheDialog() {
        _state.update { it.copy(showClearCacheDialog = false) }
    }
    
    private fun clearCache() {
        scope.launch {
            _state.update { 
                it.copy(isClearingCache = true, showClearCacheDialog = false) 
            }
            try {
                cacheRepository.clearCache()
                _state.update { it.copy(cacheSize = 0L, isClearingCache = false) }
                _effect.send(SettingsEffect.ClearCacheSuccess)
            } catch (e: Exception) {
                _state.update { it.copy(isClearingCache = false) }
                throw e
            }
        }
    }
    
    // ============= 网络设置 =============
    
    private fun navigateToNetworkConfig() {
        scope.launch {
            _effect.send(SettingsEffect.NavigateToNetworkConfig)
        }
    }
    
    // ============= 退出登录 =============
    
    private fun showLogoutDialog() {
        _state.update { it.copy(showLogoutDialog = true) }
    }
    
    private fun dismissLogoutDialog() {
        _state.update { it.copy(showLogoutDialog = false) }
    }
    
    private fun logout() {
        scope.launch {
            _state.update { it.copy(showLogoutDialog = false) }
            userRepository.logout()
            _effect.send(SettingsEffect.LogoutSuccess)
            _effect.send(SettingsEffect.NavigateToLogin)
        }
    }
}
