package com.bookd.app.screen.bookshelf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bookd.app.ui.AppPreview
import com.bookd.app.ui.AppPreviewContent

@Composable
fun BookshelfScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Bookshelf",
        )
    }
}


@AppPreview
@Composable
private fun BookshelfScreenPreview() {
    AppPreviewContent {
        BookshelfScreen()
    }
}