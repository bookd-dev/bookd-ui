package com.bookd.app.screen.main

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteDefaults
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.bookshelf
import app.composeapp.generated.resources.settings
import com.bookd.app.basic.navigation.popTransitionSpec
import com.bookd.app.basic.navigation.predictivePopTransitionSpec
import com.bookd.app.basic.navigation.transitionSpec
import com.bookd.app.screen.RouteMain.Companion.ROUTE_BOOKSHELF
import com.bookd.app.screen.RouteMain.Companion.ROUTE_SETTINGS
import com.bookd.app.screen.bookshelf.BookshelfScreen
import com.bookd.app.screen.settings.SettingsScreen
import com.bookd.app.ui.icons.Bookshelf
import com.bookd.app.ui.icons.Settings
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private sealed interface TopLevelRoute : NavKey {
    val iconOutline: ImageVector
    val iconFilled: ImageVector

    val description: StringResource
    val route: String
}

@Serializable
data object Bookshelf : TopLevelRoute {
    override val iconOutline: ImageVector = Icons.Outlined.Bookshelf
    override val iconFilled: ImageVector = Icons.Filled.Bookshelf

    override val description: StringResource = Res.string.bookshelf
    override val route: String = ROUTE_BOOKSHELF
}

@Serializable
data object Settings : TopLevelRoute {
    override val iconOutline: ImageVector = Icons.Outlined.Settings
    override val iconFilled: ImageVector = Icons.Filled.Settings

    override val description: StringResource = Res.string.settings
    override val route: String = ROUTE_SETTINGS
}

private val TOP_LEVEL_ROUTES: List<TopLevelRoute> = listOf(Bookshelf, Settings)

private fun fromRoute(route: String): TopLevelRoute = TOP_LEVEL_ROUTES.firstOrNull { it.route == route }
    ?: throw IllegalArgumentException("Route $route not found")

@Composable
fun MainScreen(
    route: String,
    onBottomBarClick: (key: String) -> Unit,
) {
    val topLevelBackStack = remember { TopLevelBackStack(fromRoute(route)) }
    val itemColors = NavigationSuiteDefaults.itemColors(
        navigationBarItemColors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
        navigationRailItemColors = NavigationRailItemDefaults.colors(indicatorColor = Color.Transparent),
        navigationDrawerItemColors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Color.Transparent)
    )

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TOP_LEVEL_ROUTES.forEach { topLevelRoute ->
                val isSelected = topLevelRoute == topLevelBackStack.topLevelKey
                item(
                    selected = isSelected,
                    onClick = {
                        topLevelBackStack.addTopLevel(topLevelRoute)
                        onBottomBarClick(topLevelRoute.route)
                    },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) topLevelRoute.iconFilled else topLevelRoute.iconOutline,
                            contentDescription = stringResource(topLevelRoute.description),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(topLevelRoute.description),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    },
                    colors = itemColors,
                )
            }
        },
    ) {
        NavDisplay(
            backStack = topLevelBackStack.backStack,
            onBack = { topLevelBackStack.removeLast() },
            transitionSpec = transitionSpec(),
            popTransitionSpec = popTransitionSpec(),
            predictivePopTransitionSpec = predictivePopTransitionSpec(),
            entryProvider = entryProvider {
                entry<Bookshelf> {
                    BookshelfScreen()
                }

                entry<Settings> {
                    SettingsScreen()
                }
            },
        )
    }
}

class TopLevelBackStack<T : Any>(startKey: T) {

    // Maintain a stack for each top level route
    private var topLevelStacks: LinkedHashMap<T, SnapshotStateList<T>> = linkedMapOf(
        startKey to mutableStateListOf(startKey)
    )

    // Expose the current top level route for consumers
    var topLevelKey by mutableStateOf(startKey)
        private set

    // Expose the back stack so it can be rendered by the NavDisplay
    val backStack = mutableStateListOf(startKey)

    private fun updateBackStack() =
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }

    fun addTopLevel(key: T) {

        // If the top level doesn't exist, add it
        if (topLevelStacks[key] == null) {
            topLevelStacks[key] = mutableStateListOf(key)
        } else {
            // Otherwise just move it to the end of the stacks
            topLevelStacks.apply {
                remove(key)?.let {
                    put(key, it)
                }
            }
        }
        topLevelKey = key
        updateBackStack()
    }

    fun add(key: T) {
        topLevelStacks[topLevelKey]?.add(key)
        updateBackStack()
    }

    fun removeLast() {
        val removedKey = topLevelStacks[topLevelKey]?.removeLastOrNull()
        // If the removed key was a top level key, remove the associated top level stack
        topLevelStacks.remove(removedKey)
        topLevelKey = topLevelStacks.keys.last()
        updateBackStack()
    }
}