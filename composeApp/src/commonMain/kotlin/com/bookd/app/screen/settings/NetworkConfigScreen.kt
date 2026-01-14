package com.bookd.app.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.cancel
import app.composeapp.generated.resources.confirm
import app.composeapp.generated.resources.local_url_config
import app.composeapp.generated.resources.local_url_config_input
import app.composeapp.generated.resources.network_config
import app.composeapp.generated.resources.remote_url_config
import app.composeapp.generated.resources.remote_url_config_input
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppPreview
import com.bookd.app.ui.theme.AppTheme
import org.jetbrains.compose.resources.stringResource

@Composable
fun NetworkConfigScreen() {

    val screenContext = rememberScreenContext()

    Dialog(
        onDismissRequest = { screenContext.navigator.navigateBack() },
    ) {
        NetworkConfigContent(
            onDismissRequest = { screenContext.navigator.navigateBack() },
            onConfirmRequest = { remoteUrl, localUrl ->

            }
        )
    }
}

@Composable
private fun NetworkConfigContent(
    onDismissRequest: () -> Unit = {},
    onConfirmRequest: (
        remoteUrl: String,
        localUrl: String
    ) -> Unit = { _, _ -> },
) {
    var remoteUrl by remember { mutableStateOf("") }
    var localUrl by remember { mutableStateOf("") }
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

        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                TextButton(
                    onClick = { onDismissRequest() },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        stringResource(Res.string.cancel),
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                VerticalDivider()

                TextButton(
                    onClick = { onConfirmRequest(localUrl, localUrl) },
                    enabled = enableConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.confirm))
                }
            }
        }
    }
}

@AppPreview
@Composable
private fun NetworkConfigPreview() {
    AppTheme {
        Box(
            modifier = Modifier.fillMaxSize().background(color = Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            NetworkConfigContent()
        }
    }
}