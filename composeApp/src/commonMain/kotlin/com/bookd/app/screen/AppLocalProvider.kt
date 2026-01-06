package com.bookd.app.screen

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.ui.NavDisplay

val LocalNavBackStack = staticCompositionLocalOf<NavBackStack<NavKey>> {
    error("No LocalNavBackStack provided")
}

