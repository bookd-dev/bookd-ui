package com.bookd.app.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.bookd.app.di.initKoin
import com.bookd.app.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplication

@Composable
@Preview
fun App() {
    KoinApplication(configuration = initKoin()) {
        AppTheme {
            AppScreen()
        }
    }
}