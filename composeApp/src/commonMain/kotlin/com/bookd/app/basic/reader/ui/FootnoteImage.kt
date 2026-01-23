package com.bookd.app.basic.reader.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.bookd.app.data.model.ContentElement


@Composable
fun FootnoteImage(
    footnote: ContentElement.Footnote,
) {
    AsyncImage(
        model = footnote.footnoteImage,
        contentDescription = footnote.footnoteSpan?.text,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit
    )
}