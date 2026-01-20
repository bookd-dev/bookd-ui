package com.bookd.app.screen.booksource.content

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.Book
import com.bookd.app.screen.booksource.component.BookListItem
import com.bookd.app.screen.booksource.component.BookListSkeletonList

/**
 * 书源书籍列表内容
 */
@Composable
fun BookSourceListContent(
    sourceId: Int,
    books: List<Book>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    error: String?,
    scrollState: LazyListState,
    tabRowHeightPx: Float,
    tabRowOffset: Float,
    onTabRowOffsetChanged: (Float) -> Unit,
    onLoadMore: () -> Unit,
    onBookClick: (Book) -> Unit = {},
) {
    // 使用 rememberUpdatedState 确保闭包中使用最新值
    val currentTabRowOffset by rememberUpdatedState(tabRowOffset)
    val currentOnChanged by rememberUpdatedState(onTabRowOffsetChanged)

    // NestedScrollConnection for Tab collapse/expand
    val nestedScrollConnection = remember(sourceId) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                // 向上滚动（delta < 0）：先折叠 Tab，再滚动列表
                if (delta < 0 && currentTabRowOffset > -tabRowHeightPx) {
                    val newOffset = (currentTabRowOffset + delta).coerceAtLeast(-tabRowHeightPx)
                    val consumed = newOffset - currentTabRowOffset
                    currentOnChanged(newOffset)
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
                if (delta > 0 && currentTabRowOffset < 0) {
                    // 检查列表是否已经在顶部
                    val atTop = scrollState.firstVisibleItemIndex == 0 &&
                            scrollState.firstVisibleItemScrollOffset == 0
                    if (atTop) {
                        val newOffset = (currentTabRowOffset + delta).coerceAtMost(0f)
                        val consumedOffset = newOffset - currentTabRowOffset
                        currentOnChanged(newOffset)
                        return Offset(0f, consumedOffset)
                    }
                }

                return Offset.Zero
            }
        }
    }
    
    // 触发加载更多
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = scrollState.layoutInfo.totalItemsCount
            lastVisibleItem != null && 
                lastVisibleItem.index >= totalItems - 3 && 
                !isLoading && 
                !isLoadingMore && 
                hasMore &&
                books.isNotEmpty()
        }
    }
    
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onLoadMore()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        when {
            // 初始加载中
            isLoading && books.isEmpty() -> {
                BookListSkeletonList(count = 5)
            }
            
            // 加载错误且无缓存数据
            error != null && books.isEmpty() -> {
                ErrorContent(
                    message = error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // 空数据
            !isLoading && books.isEmpty() -> {
                EmptyContent(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // 有数据
            else -> {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = books,
                        key = { it.id }
                    ) { book ->
                        BookListItem(
                            book = book,
                            onClick = { onBookClick(book) }
                        )
                    }
                    
                    // 加载更多指示器
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    
                    // 没有更多数据
                    if (!hasMore && books.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "没有更多了",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "此书源暂无书籍",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
    }
}
