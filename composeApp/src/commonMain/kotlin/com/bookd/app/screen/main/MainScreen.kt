package com.bookd.app.screen.main

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.bookshelf
import app.composeapp.generated.resources.compose_multiplatform
import app.composeapp.generated.resources.settings
import com.bookd.app.basic.navigation.popTransitionSpec
import com.bookd.app.basic.navigation.predictivePopTransitionSpec
import com.bookd.app.basic.navigation.transitionSpec
import com.bookd.app.screen.bookshelf.BookshelfScreen
import com.bookd.app.screen.settings.SettingsScreen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import kotlin.collections.remove

private sealed interface TopLevelRoute {
    val icon: DrawableResource

    val description: StringResource
}

//@Serializable
private data object Bookshelf : TopLevelRoute, NavKey {
    override val icon = Res.drawable.compose_multiplatform
    override val description: StringResource = Res.string.bookshelf
}

//@Serializable
private data object Settings : TopLevelRoute, NavKey {
    override val icon = Res.drawable.compose_multiplatform
    override val description: StringResource = Res.string.settings
}

//private val config = SavedStateConfiguration {
//    serializersModule = SerializersModule {
//        polymorphic(NavKey::class) {
//            subclass(Bookshelf::class, Bookshelf.serializer())
//            subclass(Settings::class, Settings.serializer())
//        }
//    }
//}

private val TOP_LEVEL_ROUTES: List<TopLevelRoute> = listOf(Bookshelf, Settings)

@Composable
fun MainScreen(route: String) {
    val topLevelBackStack = remember { TopLevelBackStack<TopLevelRoute>(Bookshelf) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                TOP_LEVEL_ROUTES.forEach { topLevelRoute ->
                    val isSelected = topLevelRoute == topLevelBackStack.topLevelKey
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            topLevelBackStack.addTopLevel(topLevelRoute)
                        },
                        icon = {
                            Icon(
                                imageVector = vectorResource(topLevelRoute.icon),
                                contentDescription = stringResource(topLevelRoute.description),
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = stringResource(topLevelRoute.description),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            )
                        }
                    )
                }
            }
        }
    ) { _ ->
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