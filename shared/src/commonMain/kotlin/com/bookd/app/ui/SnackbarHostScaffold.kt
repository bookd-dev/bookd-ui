package com.bookd.app.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import app.composeapp.generated.resources.*
import com.bookd.app.data.api.NetworkException
import com.bookd.app.data.api.NoNetworkConfigException
import com.bookd.app.data.api.NotAuthenticatedException
import com.bookd.app.data.api.TokenExpiredException
import com.bookd.app.screen.LocalSnackbarHostState
import kotlinx.coroutines.flow.Flow
import org.jetbrains.compose.resources.stringResource

@Composable
fun SnackbarHostScaffold(
    errorEvents: Flow<Throwable>,
    content: @Composable (PaddingValues) -> Unit,
) {
    val snackbarHostState = LocalSnackbarHostState.current
    val noNetworkConfigMessage = stringResource(Res.string.error_no_network_config)
    val tokenExpiredMessage = stringResource(Res.string.error_token_expired)
    val notAuthenticatedMessage = stringResource(Res.string.error_not_authenticated)
    val networkErrorMessage = stringResource(Res.string.error_network)
    val unknownErrorMessage = stringResource(Res.string.error_unknown)

    LaunchedEffect(errorEvents) {
        errorEvents.collect { throwable ->
            val message = when (throwable) {
                is NoNetworkConfigException -> noNetworkConfigMessage
                is TokenExpiredException -> tokenExpiredMessage
                is NotAuthenticatedException -> notAuthenticatedMessage
                is NetworkException -> throwable.message ?: networkErrorMessage
                else -> throwable.message ?: unknownErrorMessage
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        content = content,
    )
}
