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
import androidx.compose.ui.graphics.Color
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
    
    // 独立的折叠状态，不随 page 切换重置
    var isCollapsed by remember { mutableStateOf(false) }
    
    // 监听当前 page 的滚动，更新折叠状态
    val currentScrollState = scrollStates[pagerState.currentPage]
    val collapseProgress by remember(currentScrollState) {
        derivedStateOf {
            val firstVisible = currentScrollState.firstVisibleItemIndex
            val offset = currentScrollState.firstVisibleItemScrollOffset
            if (firstVisible > 0) 1f
            else (offset / 40f).coerceIn(0f, 1f)
        }
    }
    
    // 只在滚动时更新折叠状态，page 切换不影响
    // 添加防抖：只有明确的折叠/展开动作才更新状态
    LaunchedEffect(currentScrollState.isScrollInProgress, collapseProgress) {
        if (currentScrollState.isScrollInProgress) {
            // 只在接近两端时才更新，避免中间状态闪烁
            when {
                collapseProgress > 0.8f -> isCollapsed = true
                collapseProgress < 0.2f -> isCollapsed = false
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
            LazyColumn(state = scrollStates[page]) {
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