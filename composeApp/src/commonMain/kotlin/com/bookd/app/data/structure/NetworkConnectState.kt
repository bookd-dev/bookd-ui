package com.bookd.app.data.structure

import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.failed
import app.composeapp.generated.resources.pending
import app.composeapp.generated.resources.success
import org.jetbrains.compose.resources.StringResource

enum class NetworkConnectState(
    val text: StringResource,
) {
    Pending(Res.string.pending),

    Failed(Res.string.failed),

    Success(Res.string.success),
}