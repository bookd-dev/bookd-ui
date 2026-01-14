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
import app.composeapp.generated.resources.network
import app.composeapp.generated.resources.settings
import com.bookd.app.basic.extension.noRippleClickable
import com.bookd.app.data.vm.BookshelfViewModel
import com.bookd.app.ui.AppPreview
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.icons.BooklistMore
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun BookshelfScreen() {
    val viewModel = koinViewModel<BookshelfViewModel>()

    BookshelfContent(

    )
}

@Composable
private fun BookshelfContent() {
    val pagerState = rememberPagerState(initialPage = 0) { 30 }
    val coroutineScope = rememberCoroutineScope()
    
    // 每个 page 独立的滚动状态
    val scrollStates = remember { List(30) { LazyListState() } }
    
    // Tab 行高度
    val tabRowHeight = 40.dp
    val density = LocalDensity.current
    val tabRowHeightPx = with(density) { tabRowHeight.toPx() }
    
    // 使用 offset 来平滑控制折叠，避免二元状态导致的闪烁
    var tabRowOffset by remember { mutableStateOf(0f) }
    
    // 折叠状态由 offset 派生，避免中间状态
    val isCollapsed by remember { derivedStateOf { tabRowOffset <= -tabRowHeightPx * 0.5f } }
    
    // 获取当前页的滚动状态
    val currentScrollState = scrollStates[pagerState.currentPage]
    
    // NestedScroll 处理惯性滚动
    val nestedScrollConnection = remember(currentScrollState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                
                // 向上滚动（delta < 0）：先折叠 Tab，再滚动列表
                if (delta < 0 && tabRowOffset > -tabRowHeightPx) {
                    val consumed = (tabRowOffset + delta).coerceAtLeast(-tabRowHeightPx) - tabRowOffset
                    tabRowOffset += consumed
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
                if (delta > 0 && tabRowOffset < 0) {
                    // 检查列表是否已经在顶部
                    val atTop = currentScrollState.firstVisibleItemIndex == 0 &&
                            currentScrollState.firstVisibleItemScrollOffset == 0
                    if (atTop) {
                        val newOffset = (tabRowOffset + delta).coerceAtMost(0f)
                        val consumed = newOffset - tabRowOffset
                        tabRowOffset = newOffset
                        return Offset(0f, consumed)
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
                    pagerState.animateScrollToPage(it)
                }
            },
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
    onMenuClick: () -> Unit = {},
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
                    DropdownMenuItem(
                        text = { Text(stringResource(Res.string.network)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        onClick = {
                            expanded = false
                        }
                    )
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