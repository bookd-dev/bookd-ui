package com.bookd.app.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import com.bookd.app.basic.navigation.Navigator

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("No LocalNavigator provided")
}

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No LocalSnackbarHostState provided")
}