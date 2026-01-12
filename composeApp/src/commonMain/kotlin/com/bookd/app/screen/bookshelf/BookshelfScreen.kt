package com.bookd.app.screen.bookshelf

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.booklist
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
    val scrollState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 计算折叠进度 (0f = 展开, 1f = 折叠)
    val collapseProgress by remember {
        derivedStateOf {
            val firstVisible = scrollState.firstVisibleItemIndex
            val offset = scrollState.firstVisibleItemScrollOffset
            if (firstVisible > 0) 1f
            else (offset / 40f).coerceIn(0f, 1f) // 40 = 折叠区域高度
        }
    }

    Column {
        // 固定 Header - 始终显示
        BookshelfHeader(
            pagerState = pagerState,
            collapseProgress = collapseProgress,
            onBookSourceChange = {
                coroutineScope.launch {
                    pagerState.scrollToPage(it)
                }
            },
        )

        HorizontalPager(
            state = pagerState,
        ) {
            LazyColumn(state = scrollState) {
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
    collapseProgress: Float, // 0f = 展开, 1f = 折叠
    onBookSourceChange: (Int) -> Unit = {},
    onBooklistClick: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    val isCollapsed = collapseProgress > 0.5f

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
                        text = { Text("配置地址") },
                        onClick = {
                            expanded = false
                        }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("暂时方式") },
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