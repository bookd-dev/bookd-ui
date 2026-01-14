package com.bookd.app.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.bookd.app.basic.navigation.Navigator
import kotlinx.coroutines.CoroutineScope
import org.koin.compose.currentKoinScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.core.scope.Scope
import org.koin.viewmodel.defaultExtras

private interface IAppScreenContext {
    val navigator: Navigator

    val coroutineScope: CoroutineScope
}

data class AppScreenContext <T : ViewModel> (
    val viewModel: T,

    override val navigator: Navigator,

    override val coroutineScope: CoroutineScope,
) : IAppScreenContext

data class NonVMAppScreenContext(
    override val navigator: Navigator,

    override val coroutineScope: CoroutineScope,
) : IAppScreenContext


/**
 * viewmodel parameters copy from [koinViewModel]
 */
@Composable
inline fun <reified T : ViewModel> rememberScreenContext(
    qualifier: Qualifier? = null,
    viewModelStoreOwner: ViewModelStoreOwner =  LocalViewModelStoreOwner.current ?: error("No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"),
    key: String? = null,
    extras: CreationExtras = defaultExtras(viewModelStoreOwner),
    scope: Scope = currentKoinScope(),
    noinline parameters: ParametersDefinition? = null,
): AppScreenContext<T> {
    val viewModel = koinViewModel<T>(
        qualifier = qualifier,
        viewModelStoreOwner = viewModelStoreOwner,
        key = key,
        extras = extras,
        scope = scope,
        parameters = parameters,
    )

    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()

    return AppScreenContext(
        viewModel = viewModel,
        navigator = navigator,
        coroutineScope = coroutineScope,
    )
}

@Composable
fun rememberScreenContext(): NonVMAppScreenContext {
    val navigator = LocalNavigator.current
    val coroutineScope = rememberCoroutineScope()
    return NonVMAppScreenContext(
        navigator = navigator,
        coroutineScope = coroutineScope
    )
}

