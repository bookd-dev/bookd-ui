package com.bookd.app.screen.bookshelf

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import com.bookd.app.data.structure.BookshelfMenu
import com.bookd.app.data.vm.BookshelfViewModel
import com.bookd.app.screen.RouteNetworkConfig
import com.bookd.app.screen.bookshelf.content.BookshelfHeaderContent
import com.bookd.app.screen.bookshelf.content.BookshelfListContent
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.AppVerticalZHPreview

@Composable
fun BookshelfScreen() {
    val screenContext = rememberScreenContext<BookshelfViewModel>()

    BookshelfContent(
        onMenuClick = {
            when (it) {
                BookshelfMenu.NetworkConfig -> screenContext.navigator.navigateUnconditionally(RouteNetworkConfig)
            }
        }
    )
}

@Composable
private fun BookshelfContent(
    onMenuClick: (entry: BookshelfMenu) -> Unit = {},
) {
    Column {
        // 固定 Header - 书架专用
        BookshelfHeaderContent(
            onMenuClick = onMenuClick
        )

        // 书架列表内容
        BookshelfListContent()
    }
}


@AppVerticalZHPreview
@Composable
private fun BookshelfScreenPreview() {
    AppPreviewContent {
        BookshelfContent()
    }
}
