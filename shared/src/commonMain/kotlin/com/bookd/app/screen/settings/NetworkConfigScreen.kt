package com.bookd.app.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.composeapp.generated.resources.*
import com.bookd.app.data.repository.NetworkConfigRepository
import com.bookd.app.data.structure.UrlTestState
import com.bookd.app.data.structure.color
import com.bookd.app.data.structure.text
import com.bookd.app.data.vm.AppViewModel
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppVerticalZHPreview
import com.bookd.app.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun NetworkConfigScreen(viewModel: AppViewModel) {

    val screenContext = rememberScreenContext()
    val networkConfigRepository = koinInject<NetworkConfigRepository>()
    
    // 从 AppViewModel 收集全局网络状态
    val networkState by viewModel.networkState.collectAsState()
    val externalTestState = networkState.externalUrlState
    val internalTestState = networkState.internalUrlState

    Dialog(
        onDismissRequest = { screenContext.navigator.navigateBack() },
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        NetworkConfigContent(
            initialRemoteUrl = networkConfigRepository.externalUrl,
            initialLocalUrl = networkConfigRepository.internalUrl,
            remoteTestState = externalTestState,
            localTestState = internalTestState,
            onDismissRequest = { screenContext.navigator.navigateBack() },
            onConfirmRequest = { remoteUrl, localUrl ->
                networkConfigRepository.externalUrl = remoteUrl
                networkConfigRepository.internalUrl = localUrl
                viewModel.retryNetwork()
                screenContext.navigator.navigateBack()
            },
            onTestRequest = { remoteUrl, localUrl ->
                networkConfigRepository.externalUrl = remoteUrl
                networkConfigRepository.internalUrl = localUrl
                viewModel.retryNetwork()
            }
        )
    }
}

@Composable
private fun NetworkConfigContent(
    initialRemoteUrl: String = "",
    initialLocalUrl: String = "",
    remoteTestState: UrlTestState = UrlTestState.Pending,
    localTestState: UrlTestState = UrlTestState.Pending,
    onDismissRequest: () -> Unit = {},
    onConfirmRequest: (remoteUrl: String, localUrl: String) -> Unit = { _, _ -> },
    onTestRequest: (remoteUrl: String, localUrl: String) -> Unit = { _, _ -> }
) {
    var remoteUrl by remember { mutableStateOf(initialRemoteUrl) }
    var localUrl by remember { mutableStateOf(initialLocalUrl) }
    
    // 判断 URL 是否与已保存的配置不同（用户正在编辑中）
    val isRemoteUrlEdited = remoteUrl != initialRemoteUrl
    val isLocalUrlEdited = localUrl != initialLocalUrl
    
    // 如果 URL 被编辑，显示 Pending；否则显示全局状态
    val displayRemoteState = if (isRemoteUrlEdited) UrlTestState.Pending else remoteTestState
    val displayLocalState = if (isLocalUrlEdited) UrlTestState.Pending else localTestState

    val enableConfirm by remember {
        derivedStateOf {
            remoteUrl.isNotBlank() || localUrl.isNotBlank()
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .width(400.dp)
            .wrapContentHeight()
            .imePadding()
            .clip(RoundedCornerShape(8.dp))
            .background(color = MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = stringResource(Res.string.network_config),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        )

        OutlinedTextField(
            value = remoteUrl,
            onValueChange = { remoteUrl = it },
            label = {
                Text(stringResource(Res.string.remote_url_config))
            },
            placeholder = {
                Text(stringResource(Res.string.remote_url_config_input))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        OutlinedTextField(
            value = localUrl,
            onValueChange = { localUrl = it },
            label = {
                Text(stringResource(Res.string.local_url_config))
            },
            placeholder = {
                Text(stringResource(Res.string.local_url_config_input))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(Res.string.test),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row {
                    Text(
                        text = stringResource(Res.string.remote_url),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.alignByBaseline()
                    )

                    Text(
                        text = stringResource(displayRemoteState.text),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = displayRemoteState.color(),
                        modifier = Modifier.alignByBaseline()
                    )
                }

                Row {
                    Text(
                        text = stringResource(Res.string.local_url),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.alignByBaseline()
                    )

                    Text(
                        text = stringResource(displayLocalState.text),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = displayLocalState.color(),
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }

            TextButton(
                enabled = enableConfirm,
                onClick = { onTestRequest(remoteUrl, localUrl) },
                modifier = Modifier
                    .border(
                        color = if (enableConfirm)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.surfaceVariant,
                        width = 1.dp,
                        shape = MaterialTheme.shapes.medium
                    ),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues()
            ) {
                Text(stringResource(Res.string.test))
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                TextButton(
                    onClick = { onDismissRequest() },
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    contentPadding = PaddingValues(),
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                VerticalDivider()

                TextButton(
                    onClick = { onConfirmRequest(remoteUrl, localUrl) },
                    enabled = enableConfirm,
                    modifier = Modifier.weight(1f),
                    shape = RectangleShape,
                    contentPadding = PaddingValues()
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            }
        }
    }
}

@AppVerticalZHPreview
@Composable
private fun NetworkConfigPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(color = MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            NetworkConfigContent(
                initialRemoteUrl = "",
                initialLocalUrl = "",
            )
        }
    }
}
