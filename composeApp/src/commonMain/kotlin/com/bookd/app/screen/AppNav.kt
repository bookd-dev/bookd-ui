package com.bookd.app.screen

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
data class RouteMain(val route: String) : NavKey

@Serializable
data object RouteSignIn : NavKey

val config = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(RouteMain::class, RouteMain.serializer())
            subclass(RouteSignIn::class, RouteSignIn.serializer())
        }
    }
}
