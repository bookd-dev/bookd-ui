package com.bookd.app.screen

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data class RouteMain(val route: String?) : NavKey {


    companion object {
        const val ROUTE_BOOKSHELF = "bookshelf"
        const val ROUTE_SETTINGS = "settings"

        val RouteBookshelf = RouteMain(ROUTE_BOOKSHELF)
        val RouteSettings = RouteMain(ROUTE_SETTINGS)

    }
}

@Serializable
data object RouteSignIn : NavKey

@Serializable
data object RouteNetworkConfig : NavKey

@Serializable
data object RouteSearchBook : NavKey

val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(RouteMain::class, RouteMain.serializer())
            subclass(RouteSignIn::class, RouteSignIn.serializer())
            subclass(RouteNetworkConfig::class, RouteNetworkConfig.serializer())
            subclass(RouteSearchBook::class, RouteSearchBook.serializer())
        }
    }
}
