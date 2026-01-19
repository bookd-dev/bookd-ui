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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import app.composeapp.generated.resources.*
import com.bookd.app.data.repository.NetworkConfigRepository
import com.bookd.app.data.structure.NetworkConnectState
import com.bookd.app.data.structure.color
import com.bookd.app.data.vm.AppViewModel
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppVerticalZHPreview
import com.bookd.app.ui.theme.AppTheme
import com.bookd.app.ui.theme.Gray500
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun NetworkConfigScreen(viewModel: AppViewModel) {

    val screenContext = rememberScreenContext()
    val networkConfigRepository = koinInject<NetworkConfigRepository>()
    val httpClient = koinInject<HttpClient>()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
    }

    Dialog(
        onDismissRequest = { screenContext.navigator.navigateBack() },
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        NetworkConfigContent(
            initialRemoteUrl = networkConfigRepository.externalUrl,
            initialLocalUrl = networkConfigRepository.internalUrl,
            onDismissRequest = { screenContext.navigator.navigateBack() },
            onConfirmRequest = { remoteUrl, localUrl ->
                networkConfigRepository.externalUrl = remoteUrl
                networkConfigRepository.internalUrl = localUrl
                viewModel.retryNetwork()
                screenContext.navigator.navigateBack()
            },
            onConnectTestRequest = { remoteUrl, localUrl, onResult ->
                coroutineScope.launch {
                    // 测试远程 URL
                    if (remoteUrl.isNotBlank()) {
                        onResult(remoteUrl, testConnection(httpClient, remoteUrl))
                    }
                    // 测试本地 URL
                    if (localUrl.isNotBlank()) {
                        onResult(localUrl, testConnection(httpClient, localUrl))
                    }
                }
            }
        )
    }
}

private suspend fun testConnection(httpClient: HttpClient, url: String): NetworkConnectState {
    return try {
        withTimeout(5000L) {
            val response = httpClient.get("$url/api/health")
            if (response.status.isSuccess()) NetworkConnectState.Success else NetworkConnectState.Failed
        }
    } catch (e: Exception) {
        println("Connection test failed: ${e.message}")  // 添加日志
        NetworkConnectState.Failed
    }
}

@Composable
private fun NetworkConfigContent(
    initialRemoteUrl: String = "",
    initialLocalUrl: String = "",
    onDismissRequest: () -> Unit = {},
    onConfirmRequest: (remoteUrl: String, localUrl: String) -> Unit = { _, _ -> },
    onConnectTestRequest: (
        remoteUrl: String,
        localUrl: String,
        onResult: (url: String, state: NetworkConnectState) -> Unit
    ) -> Unit = { _, _, _ -> }
) {
    var remoteUrl by remember { mutableStateOf(initialRemoteUrl) }
    var localUrl by remember { mutableStateOf(initialLocalUrl) }
    var remoteTestState by remember { mutableStateOf(NetworkConnectState.Pending) }
    var localTestState by remember { mutableStateOf(NetworkConnectState.Pending) }
    var isTesting by remember { mutableStateOf(false) }

    val enableConfirm by remember {
        derivedStateOf {
            remoteUrl.isNotBlank() || localUrl.isNotBlank()
        }
    }

    // URL 变化时重置测试状态
    LaunchedEffect(remoteUrl) {
        remoteTestState = NetworkConnectState.Pending
    }
    LaunchedEffect(localUrl) {
        localTestState = NetworkConnectState.Pending
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
                color = Gray500
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
                        text = stringResource(remoteTestState.text),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = remoteTestState.color(),
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
                        text = stringResource(localTestState.text),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = localTestState.color(),
                        modifier = Modifier.alignByBaseline()
                    )
                }
            }

            TextButton(
                enabled = enableConfirm && !isTesting,
                onClick = {
                    isTesting = true
                    onConnectTestRequest(remoteUrl, localUrl) { url, state ->
                        when (url) {
                            remoteUrl -> remoteTestState = state
                            localUrl -> localTestState = state
                        }
                        // 两个都测试完成后重置 isTesting
                        if ((remoteUrl.isBlank() || remoteTestState != NetworkConnectState.Pending) &&
                            (localUrl.isBlank() || localTestState != NetworkConnectState.Pending)) {
                            isTesting = false
                        }
                    }
                },
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
            modifier = Modifier.fillMaxSize().background(color = Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            NetworkConfigContent(
                initialRemoteUrl = "",
                initialLocalUrl = "",
            )
        }
    }
}