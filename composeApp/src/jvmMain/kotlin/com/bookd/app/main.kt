package com.bookd.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.bookd.app.screen.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "app",
    ) {
        App()
    }
}