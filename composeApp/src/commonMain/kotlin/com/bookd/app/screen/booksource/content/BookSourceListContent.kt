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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.no_more_data
import app.composeapp.generated.resources.source_no_books
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource

/**
 * 书源书籍列表内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSourceListContent(
    sourceId: Int,
    books: List<Book>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
    hasMore: Boolean,
    error: String?,
    scrollState: LazyListState,
    tabRowHeightPx: Float,
    tabRowOffset: Float,
    onTabRowOffsetChanged: (Float) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onBookClick: (Book) -> Unit = {},
) {
    // 使用 rememberUpdatedState 确保闭包中使用最新值
    val currentTabRowOffset by rememberUpdatedState(tabRowOffset)
    val currentOnChanged by rememberUpdatedState(onTabRowOffsetChanged)
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val currentIsLoading by rememberUpdatedState(isLoading)
    val currentIsLoadingMore by rememberUpdatedState(isLoadingMore)
    val currentHasMore by rememberUpdatedState(hasMore)
    val currentBooksSize by rememberUpdatedState(books.size)

    // NestedScrollConnection for Tab collapse/expand
    val nestedScrollConnection = androidx.compose.runtime.remember(sourceId) {
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
    
    // 使用 snapshotFlow 监听滚动状态变化，触发加载更多
    LaunchedEffect(scrollState, sourceId) {
        snapshotFlow {
            val layoutInfo = scrollState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            
            // 判断是否需要加载更多
            lastVisibleItem != null &&
                totalItems > 0 &&
                lastVisibleItem.index >= totalItems - 3
        }
        .distinctUntilChanged()
        .filter { it } // 只在需要加载更多时触发
        .collect {
            // 在 collect 内部检查最新状态
            if (!currentIsLoading && !currentIsLoadingMore && currentHasMore && currentBooksSize > 0) {
                currentOnLoadMore()
            }
        }
    }

    // 下拉刷新状态
    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        when {
            // 初始加载中（非刷新状态）
            isLoading && books.isEmpty() && !isRefreshing -> {
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
            !isLoading && books.isEmpty() && !isRefreshing -> {
                EmptyContent(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            // 有数据或正在刷新
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
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
                            item(key = "loading_more") {
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
                        if (!hasMore && books.isNotEmpty() && !isLoadingMore) {
                            item(key = "no_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(Res.string.no_more_data),
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
}

@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.source_no_books),
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
