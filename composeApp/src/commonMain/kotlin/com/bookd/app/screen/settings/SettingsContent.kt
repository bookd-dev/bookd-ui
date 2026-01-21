package com.bookd.app.screen.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.*
import com.bookd.app.AppBuildConfig
import com.bookd.app.data.vm.SettingsIntent
import com.bookd.app.data.vm.SettingsState
import com.bookd.app.screen.settings.component.SettingItem
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.AppVerticalZHPreview
import org.jetbrains.compose.resources.stringResource

/**
 * 设置页面 Content 层
 * 
 * 纯 UI 组件，可预览
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    state: SettingsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (SettingsIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // 网络设置
            SettingItem(
                title = stringResource(Res.string.settings_network),
                icon = Icons.Default.Cloud,
                onClick = { onIntent(SettingsIntent.ClickNetworkConfig) },
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // 缓存管理
            SettingItem(
                title = stringResource(Res.string.settings_cache),
                subtitle = if (state.isLoadingCacheSize) {
                    stringResource(Res.string.cache_calculating)
                } else {
                    formatCacheSize(state.cacheSize)
                },
                icon = Icons.Default.Storage,
                trailing = if (state.isClearingCache) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    }
                } else null,
                onClick = { onIntent(SettingsIntent.ClickClearCache) },
            )
            
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            
            // 退出登录
            SettingItem(
                title = stringResource(Res.string.settings_logout),
                icon = Icons.AutoMirrored.Filled.Logout,
                onClick = { onIntent(SettingsIntent.ClickLogout) },
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // 关于 - 版本号
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.app_version, AppBuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    
    // 清除缓存确认对话框
    if (state.showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissClearCacheDialog) },
            title = { Text(stringResource(Res.string.cache_clear_confirm_title)) },
            text = { Text(stringResource(Res.string.cache_clear_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.ConfirmClearCache) }) {
                    Text(stringResource(Res.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DismissClearCacheDialog) }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
    
    // 退出登录确认对话框
    if (state.showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { onIntent(SettingsIntent.DismissLogoutDialog) },
            title = { Text(stringResource(Res.string.logout_confirm_title)) },
            text = { Text(stringResource(Res.string.logout_confirm_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(SettingsIntent.ConfirmLogout) }) {
                    Text(stringResource(Res.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(SettingsIntent.DismissLogoutDialog) }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

/**
 * 格式化缓存大小
 */
@Composable
private fun formatCacheSize(bytes: Long): String {
    return when {
        bytes < 1024 -> stringResource(Res.string.file_size_bytes, bytes.toInt())
        bytes < 1024 * 1024 -> stringResource(Res.string.file_size_kb, (bytes / 1024).toInt())
        bytes < 1024 * 1024 * 1024 -> {
            val mb = bytes / (1024.0 * 1024)
            val formatted = ((mb * 10).toLong() / 10.0).toString()
            stringResource(Res.string.file_size_mb, formatted)
        }
        else -> {
            val gb = bytes / (1024.0 * 1024 * 1024)
            val formatted = ((gb * 100).toLong() / 100.0).toString()
            stringResource(Res.string.file_size_gb, formatted)
        }
    }
}

@AppVerticalZHPreview
@Composable
private fun SettingsContentPreview() {
    AppPreviewContent {
        SettingsContent(
            state = SettingsState(
                cacheSize = 1024 * 1024 * 50, // 50 MB
                isLoadingCacheSize = false,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}
