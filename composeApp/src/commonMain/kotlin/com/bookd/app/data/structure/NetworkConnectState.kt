package com.bookd.app.data.structure

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.failed
import app.composeapp.generated.resources.pending
import app.composeapp.generated.resources.success
import com.bookd.app.ui.theme.Gray500
import org.jetbrains.compose.resources.StringResource

enum class NetworkConnectState(
    val text: StringResource,
) {
    Pending(Res.string.pending),

    Failed(Res.string.failed),

    Success(Res.string.success),
}

@Composable
fun NetworkConnectState.color(): Color = when (this) {
    NetworkConnectState.Pending -> Gray500
    NetworkConnectState.Success -> Color(0xFF4CAF50)
    NetworkConnectState.Failed -> Color(0xFFF44336)
}