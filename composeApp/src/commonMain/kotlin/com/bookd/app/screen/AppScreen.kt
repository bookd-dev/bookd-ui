package com.bookd.app.screen

import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.bookd.app.basic.navigation.popTransitionSpec
import com.bookd.app.basic.navigation.predictivePopTransitionSpec
import com.bookd.app.basic.navigation.transitionSpec
import com.bookd.app.data.vm.AppViewModel
import com.bookd.app.ui.ErrorHandlerScaffold
import com.bookd.app.screen.RouteMain.Companion.ROUTE_BOOKSHELF
import com.bookd.app.screen.RouteMain.Companion.RouteBookshelf
import com.bookd.app.screen.main.MainScreen
import com.bookd.app.screen.sign.SignInScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppScreen() {
    val viewModel = koinViewModel<AppViewModel>()
    val backStack = rememberNavBackStack(config, RouteBookshelf)

    val error by viewModel.error.collectAsStateWithLifecycle()
    var currentRouteMainKey by remember { mutableStateOf(ROUTE_BOOKSHELF) }

    LaunchedEffect(Unit) {
        viewModel.startNetworkMonitor()
    }

    CompositionLocalProvider(
        LocalNavBackStack provides backStack
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
               }
           )
       }
    }
}