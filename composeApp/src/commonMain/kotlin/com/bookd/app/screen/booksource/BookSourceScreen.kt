package com.bookd.app.screen.booksource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bookd.app.data.structure.BookSourceMenu
import com.bookd.app.data.vm.BookSourceViewModel
import com.bookd.app.screen.RouteSearchBook
import com.bookd.app.screen.booksource.content.BookSourceHeaderContent
import com.bookd.app.screen.booksource.content.BookSourceListContent
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.AppVerticalZHPreview
import kotlinx.coroutines.launch

@Composable
fun BookSourceScreen() {
    val screenContext = rememberScreenContext<BookSourceViewModel>()

    BookSourceContent(
        initialPage = screenContext.viewModel.currentPage,
        tabRowOffset = screenContext.viewModel.tabRowOffset,
        onPageChanged = { screenContext.viewModel.currentPage = it },
        onTabRowOffsetChanged = { screenContext.viewModel.tabRowOffset = it },
        onGetScrollState = { page -> screenContext.viewModel.getScrollState(page) },
        onMenuClick = {
            when (it) {
                BookSourceMenu.SearchBook -> screenContext.navigator.navigateTo(RouteSearchBook)
            }
        }
    )
}

@Composable
private fun BookSourceContent(
    initialPage: Int = 0,
    tabRowOffset: Float = 0f,
    onPageChanged: (Int) -> Unit = {},
    onTabRowOffsetChanged: (Float) -> Unit = {},
    onGetScrollState: (Int) -> LazyListState = { LazyListState() },
    onMenuClick: (entry: BookSourceMenu) -> Unit = {},
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { 30 }
    val coroutineScope = rememberCoroutineScope()

    // 同步 pagerState 到外部
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }

    // Tab 行高度
    val tabRowHeightPx = with(LocalDensity.current) { 48.dp.toPx() }
    // 本地维护 offset 用于 UI，同步到外部
    var localTabRowOffset by remember { mutableFloatStateOf(tabRowOffset) }
    // 折叠状态由 offset 派生
    val isCollapsed by remember { derivedStateOf { localTabRowOffset <= -tabRowHeightPx * 0.5f } }

    Column {
        // Header - 包含数据源 Tabs 和菜单
        BookSourceHeaderContent(
            pagerState = pagerState,
            isCollapsed = isCollapsed,
            onBookSourceChange = {
                coroutineScope.launch {
                    pagerState.scrollToPage(it)
                }
            },
            onMenuClick = onMenuClick
        )

        // 数据源内容 - HorizontalPager
        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,  // 只预加载相邻 1 页，减少初始渲染开销
        ) { page ->
            BookSourceListContent(
                page = page,
                scrollState = onGetScrollState(page),  // 按需获取 scrollState
                tabRowHeightPx = tabRowHeightPx,
                tabRowOffset = localTabRowOffset,
                onTabRowOffsetChanged = {
                    localTabRowOffset = it
                    onTabRowOffsetChanged(it)
                }
            )
        }
    }
}

@AppVerticalZHPreview
@Composable
private fun BookSourceScreenPreview() {
    AppPreviewContent {
        BookSourceContent()
    }
}
