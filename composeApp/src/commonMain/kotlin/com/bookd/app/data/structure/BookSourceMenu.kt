package com.bookd.app.data.structure

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.search_book
import org.jetbrains.compose.resources.StringResource

enum class BookSourceMenu(
    val text: StringResource,
    val icon: ImageVector? = null,
    val iconSize: Dp = 16.dp,
) {

    SearchBook(
        text = Res.string.search_book,
        icon = Icons.Outlined.Search,
    ),
}
