package com.bookd.app.screen

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.error_no_network_config
import app.composeapp.generated.resources.goto
import com.bookd.app.basic.navigation.popTransitionSpec
import com.bookd.app.basic.navigation.predictivePopTransitionSpec
import com.bookd.app.basic.navigation.rememberNavigationInterceptor
import com.bookd.app.basic.navigation.transitionSpec
import com.bookd.app.data.vm.AppViewModel
import com.bookd.app.ui.SnackbarHostScaffold
import com.bookd.app.screen.RouteMain.Companion.ROUTE_BOOKSHELF
import com.bookd.app.screen.RouteMain.Companion.RouteBookshelf
import com.bookd.app.screen.main.MainScreen
import com.bookd.app.screen.settings.NetworkConfigScreen
import com.bookd.app.screen.sign.SignInScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun AppScreen() {
    val viewModel = koinViewModel<AppViewModel>()
    val backStack = rememberNavBackStack(config, RouteBookshelf)

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var currentRouteMainKey by remember { mutableStateOf(ROUTE_BOOKSHELF) }

    val noNetworkConfigMessage = stringResource(Res.string.error_no_network_config)
    val noNetworkActionLabel = stringResource(Res.string.goto)

    val error by viewModel.error.collectAsStateWithLifecycle()

    val navigator = rememberNavigationInterceptor(
        backStack = backStack,
        connectionState = viewModel.connectionState,
        authState = viewModel.authState,
        onNeedNetworkConfig = {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(message = noNetworkConfigMessage, actionLabel = noNetworkActionLabel)

                when (result) {
                    SnackbarResult.Dismissed -> { //ignore this
                    }
                    SnackbarResult.ActionPerformed -> {
                        backStack.add(RouteNetworkConfig)
                    }
                }
            }
        }
    )


    CompositionLocalProvider(
        LocalNavigator provides navigator,
        LocalSnackbarHostState provides snackbarHostState,
    ) {
       SnackbarHostScaffold(error) {
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