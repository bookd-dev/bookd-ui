package com.bookd.app.screen.bookshelf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.booklist
import app.composeapp.generated.resources.settings
import com.bookd.app.basic.extension.noRippleClickable
import com.bookd.app.data.structure.BookshelfMenu
import com.bookd.app.data.vm.BookshelfViewModel
import com.bookd.app.screen.RouteNetworkConfig
import com.bookd.app.screen.RouteSearchBook
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppPreview
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.icons.BooklistMore
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookshelfScreen() {
    val screenContext = rememberScreenContext<BookshelfViewModel>()


    BookshelfContent(
        initialPage = screenContext.viewModel.currentPage,
        scrollStates = screenContext.viewModel.scrollStates,
        tabRowOffset = screenContext.viewModel.tabRowOffset,
        onPageChanged = { screenContext.viewModel.currentPage = it },
        onTabRowOffsetChanged = { screenContext.viewModel.tabRowOffset = it },
        onBooklistClick = {
            //TODO 书架列表点击
        },
        onMenuClick = {
            //TODO 设置页点击
            when(it) {
                BookshelfMenu.SearchBook -> screenContext.navigator.navigateTo(RouteSearchBook)
                BookshelfMenu.NetworkConfig -> screenContext.navigator.navigateUnconditionally(RouteNetworkConfig)
            }
        }
    )
}

@Composable
private fun BookshelfContent(
    initialPage: Int = 0,
    scrollStates: List<LazyListState> = remember { List(30) { LazyListState() } },
    tabRowOffset: Float = 0f,
    onPageChanged: (Int) -> Unit = {},
    onTabRowOffsetChanged: (Float) -> Unit = {},
    onBooklistClick: () -> Unit = {},
    onMenuClick: (entry: BookshelfMenu) -> Unit = {},
) {
    val pagerState = rememberPagerState(initialPage = initialPage) { 30 }
    val coroutineScope = rememberCoroutineScope()
    
    // 同步 pagerState 到外部
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
    }
    
    // Tab 行高度
    val tabRowHeight = 40.dp
    val density = LocalDensity.current
    val tabRowHeightPx = with(density) { tabRowHeight.toPx() }
    
    // 本地维护 offset 用于 UI，同步到外部
    var localTabRowOffset by remember { mutableFloatStateOf(tabRowOffset) }
    
    // 折叠状态由 offset 派生
    val isCollapsed by remember { derivedStateOf { localTabRowOffset <= -tabRowHeightPx * 0.5f } }
    
    // 获取当前页的滚动状态
    val currentScrollState = scrollStates[pagerState.currentPage]
    
    // NestedScroll 处理惯性滚动
    val nestedScrollConnection = remember(currentScrollState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                
                // 向上滚动（delta < 0）：先折叠 Tab，再滚动列表
                if (delta < 0 && localTabRowOffset > -tabRowHeightPx) {
                    val consumed = (localTabRowOffset + delta).coerceAtLeast(-tabRowHeightPx) - localTabRowOffset
                    localTabRowOffset += consumed
                    onTabRowOffsetChanged(localTabRowOffset)
                    return Offset(0f, consumed)
                }
                
                return Offset.Zero
            }
            
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                
                // 向下滚动（delta > 0）：列表滚动完后展开 Tab（包括惯性滚动）
                if (delta > 0 && localTabRowOffset < 0) {
                    // 检查列表是否已经在顶部
                    val atTop = currentScrollState.firstVisibleItemIndex == 0 &&
                            currentScrollState.firstVisibleItemScrollOffset == 0
                    if (atTop) {
                        val newOffset = (localTabRowOffset + delta).coerceAtMost(0f)
                        val consumedOffset = newOffset - localTabRowOffset
                        localTabRowOffset = newOffset
                        onTabRowOffsetChanged(localTabRowOffset)
                        return Offset(0f, consumedOffset)
                    }
                }
                
                return Offset.Zero
            }
        }
    }

    Column {
        // 固定 Header - 始终显示
        BookshelfHeader(
            pagerState = pagerState,
            isCollapsed = isCollapsed,
            onBookSourceChange = {
                coroutineScope.launch {
                    pagerState.scrollToPage(it)
                }
            },
            onBooklistClick = onBooklistClick,
            onMenuClick = onMenuClick
        )

        HorizontalPager(
            state = pagerState,
        ) { page ->
            LazyColumn(
                state = scrollStates[page],
                modifier = Modifier.nestedScroll(nestedScrollConnection)
            ) {
                items(50) { i ->
                    Column(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(color = Color.Red)
                    ) {
                        Text(
                            text = "Book$i",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BookshelfHeader(
    pagerState: PagerState,
    isCollapsed: Boolean,
    onBookSourceChange: (Int) -> Unit = {},
    onBooklistClick: () -> Unit = {},
    onMenuClick: (entry: BookshelfMenu) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        // 第一行：书架按钮 (始终显示), 书籍源(折叠显示), 设置网络(始终显示)
        // 折叠时，数据源 Tabs 移到这一行

        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .height(40.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.BooklistMore,
                contentDescription = stringResource(Res.string.booklist),
                modifier = Modifier.size(24.dp).noRippleClickable { onBooklistClick() },
            )

            // 折叠时显示数据源选择
            AnimatedVisibility(
                visible = isCollapsed,
                modifier = Modifier.weight(1f),
            ) {
                BookSourceTabList(
                    pagerState = pagerState,
                    onClick = onBookSourceChange
                )
            }

            if (!isCollapsed) {
                Spacer(modifier = Modifier.weight(1f))
            }

            Box {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = stringResource(Res.string.settings),
                    modifier = Modifier.size(24.dp).noRippleClickable { expanded = true }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    BookshelfMenu.entries.forEach { entry ->
                        val text = stringResource(entry.title)
                        DropdownMenuItem(
                            text = { Text(text = text) },
                            leadingIcon = entry.icon?.let {
                                {
                                    Icon(
                                        imageVector = entry.icon,
                                        contentDescription = text,
                                        modifier = Modifier.size(entry.iconSize)
                                    )
                                }
                            },
                            onClick = {
                                onMenuClick(entry)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // 第二行：数据源 Tabs (展开时显示)
        AnimatedVisibility(
            visible = !isCollapsed,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            BookSourceTabList(
                pagerState = pagerState,
                onClick = onBookSourceChange
            )
        }
    }
}

@Composable
private fun BookSourceTabList(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    onClick: (Int) -> Unit = {}
) {

    LazyRow(
        modifier = modifier.height(32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(30) {
            Tab(
                selected = pagerState.currentPage == it,
                onClick = { onClick(it) },
            ) {
                Text("数据源$it")
            }
        }
    }
}


@AppPreview
@Composable
private fun BookshelfScreenPreview() {
    AppPreviewContent {
        BookshelfContent()
    }
}