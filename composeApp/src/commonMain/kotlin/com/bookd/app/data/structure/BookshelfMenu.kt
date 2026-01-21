package com.bookd.app.data.structure

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.add_bookshelf
import app.composeapp.generated.resources.grid_view
import app.composeapp.generated.resources.list_view
import app.composeapp.generated.resources.network_config
import org.jetbrains.compose.resources.StringResource

enum class BookshelfMenu(
    val text: StringResource,
    val icon: ImageVector? = null,
    val iconSize: Dp = 16.dp,
) {
    ToggleViewMode(
        text = Res.string.list_view, // 会根据当前模式动态变化
        icon = Icons.AutoMirrored.Outlined.ViewList,
    ),
    
    AddBookshelf(
        text = Res.string.add_bookshelf,
        icon = Icons.Outlined.Add,
    ),

    NetworkConfig(
        text = Res.string.network_config,
        icon = Icons.Outlined.NetworkPing,
    )
}
