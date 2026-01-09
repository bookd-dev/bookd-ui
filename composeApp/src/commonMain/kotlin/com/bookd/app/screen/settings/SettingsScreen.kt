package com.bookd.app.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.bookd.app.screen.LocalNavBackStack
import com.bookd.app.ui.AppPreview
import com.bookd.app.ui.AppPreviewContent

@Composable
fun SettingsScreen() {
    val navBackStack = LocalNavBackStack.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        AsyncImage(
            model = "",
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun SettingsButton() {

}

@AppPreview
@Composable
private fun SettingsContentPreview() {
    AppPreviewContent {
        SettingsScreen()
    }
}