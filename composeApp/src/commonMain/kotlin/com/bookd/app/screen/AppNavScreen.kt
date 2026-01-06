package com.bookd.app.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.bookd.app.basic.navigation.popTransitionSpec
import com.bookd.app.basic.navigation.predictivePopTransitionSpec
import com.bookd.app.basic.navigation.transitionSpec
import com.bookd.app.screen.main.MainScreen
import com.bookd.app.screen.main.RouteBookshelf
import com.bookd.app.screen.sign.SignInScreen

@Composable
fun AppNavScreen() {
    val backStack = rememberNavBackStack(config, RouteBookshelf)

    CompositionLocalProvider(
        LocalNavBackStack provides backStack
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            transitionSpec = transitionSpec(),
            popTransitionSpec = popTransitionSpec(),
            predictivePopTransitionSpec = predictivePopTransitionSpec(),
            entryProvider = entryProvider {
                entry<RouteMain> {
                    MainScreen(it.route)
                }

                entry<RouteSignIn> {
                    SignInScreen()
                }
            }
        )
    }
}