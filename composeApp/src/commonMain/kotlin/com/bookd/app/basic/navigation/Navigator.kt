package com.bookd.app.basic.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.bookd.app.data.repository.AuthState
import com.bookd.app.data.repository.ConnectionState
import com.bookd.app.screen.RouteSignIn
import kotlinx.coroutines.flow.StateFlow

/**
 * 导航拦截结果
 */
sealed class NavigationResult {
    /** 允许导航 */
    data object Allowed : NavigationResult()
    /** 需要配置网络 */
    data object NeedNetworkConfig : NavigationResult()
    /** 需要登录 */
    data object NeedLogin : NavigationResult()
}

/**
 * 导航拦截器
 * 
 * 在导航前检查网络配置和登录状态
 */
@Stable
class Navigator(
    private val backStack: NavBackStack<NavKey>,
    private val connectionState: StateFlow<ConnectionState>,
    private val authState: StateFlow<AuthState>,
    private val onNeedNetworkConfig: () -> Unit,
) {
    /**
     * 检查是否可以导航到需要认证的页面
     * 
     * @param requireAuth 是否需要认证
     * @param requireNetwork 是否需要网络
     * @return 导航检查结果
     */
    fun check(
        requireNetwork: Boolean = true,
        requireAuth: Boolean = true,
    ): NavigationResult {
        // 检查网络配置
        if (requireNetwork) {
            val connection = connectionState.value
            if (connection is ConnectionState.Offline || connection is ConnectionState.Checking) {
                return NavigationResult.NeedNetworkConfig
            }
        }
        
        // 检查登录状态
        if (requireAuth) {
            val auth = authState.value
            if (auth !is AuthState.Authenticated) {
                return NavigationResult.NeedLogin
            }
        }
        
        return NavigationResult.Allowed
    }
    
    /**
     * 尝试导航，如果检查失败则触发相应回调
     * 
     * @param destination 目标路由
     * @param requireNetwork 是否需要网络
     * @param requireAuth 是否需要认证
     * @return 是否成功导航
     */
    fun navigateTo(
        destination: NavKey,
        requireNetwork: Boolean = true,
        requireAuth: Boolean = true,
    ): Boolean {
        return when (check(requireNetwork, requireAuth)) {
            NavigationResult.Allowed -> {
                backStack.add(destination)
                true
            }
            NavigationResult.NeedNetworkConfig -> {
                onNeedNetworkConfig()
                false
            }
            NavigationResult.NeedLogin -> {
                backStack.add(RouteSignIn)
                false
            }
        }
    }
    
    /**
     * 无条件导航（不做任何检查）
     */
    fun navigateUnconditionally(destination: NavKey) {
        backStack.add(destination)
    }
    
    /**
     * 返回上一页
     */
    fun navigateBack(): NavKey? {
        return backStack.removeLastOrNull()
    }
}

/**
 * 创建并记住 NavigationInterceptor
 */
@Composable
fun rememberNavigationInterceptor(
    backStack: NavBackStack<NavKey>,
    connectionState: StateFlow<ConnectionState>,
    authState: StateFlow<AuthState>,
    onNeedNetworkConfig: () -> Unit,
): Navigator {
    return remember(backStack, connectionState, authState, onNeedNetworkConfig) {
        Navigator(
            backStack = backStack,
            connectionState = connectionState,
            authState = authState,
            onNeedNetworkConfig = onNeedNetworkConfig,
        )
    }
}
