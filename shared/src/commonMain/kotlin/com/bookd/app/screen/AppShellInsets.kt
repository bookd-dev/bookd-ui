package com.bookd.app.screen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.Modifier

internal data class AppShellSafeAreaPolicy(
    val includesTop: Boolean,
    val includesBottom: Boolean,
    val includesHorizontal: Boolean,
)

internal val AppShellOuterSafeAreaPolicy = AppShellSafeAreaPolicy(
    includesTop = true,
    includesBottom = true,
    includesHorizontal = true,
)

internal val AppShellContentSafeAreaPolicy = AppShellSafeAreaPolicy(
    includesTop = false,
    includesBottom = false,
    includesHorizontal = false,
)

internal val AppShellNoWindowInsets = WindowInsets(0, 0, 0, 0)

internal fun Modifier.appShellOuterSafeAreaPadding(): Modifier =
    systemBarsPadding().displayCutoutPadding()

internal fun Modifier.appShellContentSafeAreaPadding(): Modifier =
    this
