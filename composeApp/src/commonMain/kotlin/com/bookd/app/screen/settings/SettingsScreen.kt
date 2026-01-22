package com.bookd.app.screen.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.composeapp.generated.resources.*
import com.bookd.app.data.vm.SettingsEffect
import com.bookd.app.data.vm.SettingsViewModel
import com.bookd.app.screen.RouteNetworkConfig
import com.bookd.app.screen.RouteSignIn
import com.bookd.app.screen.rememberScreenContext
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource

/**
 * 设置页面 Screen 层
 * 
 * 处理导航和副作用
 */
@Composable
fun SettingsScreen() {
    val screenContext = rememberScreenContext<SettingsViewModel>()
    val viewModel = screenContext.viewModel
    val navigator = screenContext.navigator
    val snackbarHostState = screenContext.snackbarHostState
    
    val state by viewModel.state.collectAsState()
    
    val cacheCleared = stringResource(Res.string.cache_cleared)
    val logoutSuccess = stringResource(Res.string.logout_success)
    
    // 处理一次性效果
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SettingsEffect.NavigateToNetworkConfig -> {
                    navigator.navigateUnconditionally(RouteNetworkConfig)
                }
                is SettingsEffect.NavigateToLogin -> {
                    navigator.navigateUnconditionally(RouteSignIn)
                }
                is SettingsEffect.ClearCacheSuccess -> {
                    snackbarHostState.showSnackbar(cacheCleared)
                }
                is SettingsEffect.LogoutSuccess -> {
                    snackbarHostState.showSnackbar(logoutSuccess)
                }
            }
        }
    }
    
    SettingsContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}
