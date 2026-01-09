@file:OptIn(ExperimentalCoilApi::class)

package com.bookd.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.navigation3.runtime.rememberNavBackStack
import coil3.ColorImage
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.AsyncImagePreviewHandler
import coil3.compose.LocalAsyncImagePreviewHandler
import com.bookd.app.di.appModules
import com.bookd.app.di.initKoin
import com.bookd.app.screen.LocalNavBackStack
import com.bookd.app.screen.RouteMain.Companion.RouteBookshelf
import com.bookd.app.screen.config
import com.bookd.app.ui.theme.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.KoinApplicationPreview


@Composable
fun AppPreviewContent(
    content: @Composable () -> Unit
) {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(Color.Red.toArgb())
    }

    val backStack = rememberNavBackStack(config, RouteBookshelf)

    KoinApplicationPreview(application = { modules(appModules) }) {
        CompositionLocalProvider(
            LocalNavBackStack provides backStack,
            LocalAsyncImagePreviewHandler provides previewHandler,
        ) {
            AppTheme {
                content()
            }
        }
    }
}

@Preview(name = "1 - Vertical EN", widthDp = 1080, heightDp = 1920, locale = "en", showBackground = true)
@Preview(name = "2 - Vertical ZH", widthDp = 1080, heightDp = 1920, locale = "zh", showBackground = true)
@Preview(name = "3 - Horizontal EN", widthDp = 1920, heightDp = 1080, locale = "en", showBackground = true)
@Preview(name = "4 - Horizontal ZH", widthDp = 1920, heightDp = 1080, locale = "zh", showBackground = true)
annotation class AppPreview