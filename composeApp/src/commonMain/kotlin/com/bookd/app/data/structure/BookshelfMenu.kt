package com.bookd.app.data.structure

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.network_config
import app.composeapp.generated.resources.search_book
import org.jetbrains.compose.resources.StringResource

enum class BookshelfMenu(
    val title: StringResource,
    val icon: ImageVector? = null,
    val iconSize: Dp = 16.dp,
) {

    SearchBook(
        title = Res.string.search_book,
        icon = Icons.Outlined.Search,
    ),

    NetworkConfig(
        title = Res.string.network_config,
        icon = Icons.Outlined.NetworkPing,
    )
}