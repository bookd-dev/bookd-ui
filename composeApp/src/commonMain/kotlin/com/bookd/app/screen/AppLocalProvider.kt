package com.bookd.app.screen

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import com.bookd.app.basic.navigation.NavigationInterceptor

val LocalNavBackStack = staticCompositionLocalOf<NavBackStack<NavKey>> {
    error("No LocalNavBackStack provided")
}

val LocalNavigationInterceptor = staticCompositionLocalOf<NavigationInterceptor> {
    error("No LocalNavigationInterceptor provided")
}

