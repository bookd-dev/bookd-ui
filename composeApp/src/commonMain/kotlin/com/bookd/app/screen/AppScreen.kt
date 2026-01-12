package com.bookd.app.screen

import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import com.bookd.app.basic.navigation.popTransitionSpec
import com.bookd.app.basic.navigation.predictivePopTransitionSpec
import com.bookd.app.basic.navigation.rememberNavigationInterceptor
import com.bookd.app.basic.navigation.transitionSpec
import com.bookd.app.data.vm.AppViewModel
import com.bookd.app.ui.ErrorHandlerScaffold
import com.bookd.app.screen.RouteMain.Companion.ROUTE_BOOKSHELF
import com.bookd.app.screen.RouteMain.Companion.RouteBookshelf
import com.bookd.app.screen.main.MainScreen
import com.bookd.app.screen.settings.NetworkConfigScreen
import com.bookd.app.screen.sign.SignInScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppScreen() {
    val viewModel = koinViewModel<AppViewModel>()
    val backStack = rememberNavBackStack(config, RouteBookshelf)

    val error by viewModel.error.collectAsStateWithLifecycle()
    var currentRouteMainKey by remember { mutableStateOf(ROUTE_BOOKSHELF) }

    val navigator = rememberNavigationInterceptor(
        backStack = backStack,
        connectionState = viewModel.connectionState,
        authState = viewModel.authState,
        onNeedNetworkConfig = { backStack.add(RouteNetworkConfig) }
    )

    CompositionLocalProvider(
        LocalNavBackStack provides backStack,
        LocalNavigationInterceptor provides navigator,
    ) {
       ErrorHandlerScaffold(error = error) {
           NavDisplay(
               backStack = backStack,
               onBack = { backStack.removeLastOrNull() },
               transitionSpec = transitionSpec(),
               popTransitionSpec = popTransitionSpec(),
               predictivePopTransitionSpec = predictivePopTransitionSpec(),
               entryProvider = entryProvider {
                   entry<RouteMain> {
                       MainScreen(
                           route = currentRouteMainKey,
                           onBottomBarClick = { routeKey -> currentRouteMainKey = routeKey },
                       )
                   }

                   entry<RouteSignIn> {
                       SignInScreen()
                   }

                   entry<RouteNetworkConfig>(
                       metadata = DialogSceneStrategy.dialog(DialogProperties())
                   ) {
                       NetworkConfigScreen()
                   }
               }
           )
       }
    }
}