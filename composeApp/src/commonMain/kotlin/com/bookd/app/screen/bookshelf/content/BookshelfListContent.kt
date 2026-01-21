package com.bookd.app.screen.bookshelf.content

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.bookshelf_no_books
import app.composeapp.generated.resources.no_more_data
import com.bookd.app.data.model.BookWithProgress
import com.bookd.app.screen.bookshelf.component.BookMenuAction
import com.bookd.app.screen.bookshelf.component.BookshelfBookGridItem
import com.bookd.app.screen.bookshelf.component.BookshelfBookListItem
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource

/**
 * 书架书籍列表内容
 * 
 * 支持列表模式和瀑布流模式，支持下拉刷新
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfListContent(
    bookshelfId: Int,
    books: List<BookWithProgress>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
    isGridMode: Boolean,
    isSystemDefaultBookshelf: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onBookClick: (BookWithProgress) -> Unit = {},
    onMenuAction: (BookWithProgress, BookMenuAction) -> Unit = { _, _ -> },
) {
    // 使用 rememberUpdatedState 确保闭包中使用最新值
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    val currentIsLoading by rememberUpdatedState(isLoading)
    val currentIsLoadingMore by rememberUpdatedState(isLoadingMore)
    val currentHasMore by rememberUpdatedState(hasMore)
    val currentBooksSize by rememberUpdatedState(books.size)

    // 下拉刷新状态
    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            // 初始加载中（不显示刷新指示器）
            isLoading && books.isEmpty() && !isRefreshing -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // 空数据
            !isLoading && books.isEmpty() && !isRefreshing -> {
                EmptyContent(modifier = Modifier.align(Alignment.Center))
            }
            
            // 有数据或正在刷新
            else -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (isGridMode) {
                        // 瀑布流/网格模式
                        GridContent(
                            bookshelfId = bookshelfId,
                            books = books,
                            isLoadingMore = isLoadingMore,
                            isSystemDefaultBookshelf = isSystemDefaultBookshelf,
                            hasMore = hasMore,
                            onLoadMore = { 
                                if (!currentIsLoading && !currentIsLoadingMore && currentHasMore && currentBooksSize > 0) {
                                    currentOnLoadMore()
                                }
                            },
                            onBookClick = onBookClick,
                            onMenuAction = onMenuAction
                        )
                    } else {
                        // 列表模式
                        ListContent(
                            bookshelfId = bookshelfId,
                            books = books,
                            isLoadingMore = isLoadingMore,
                            isSystemDefaultBookshelf = isSystemDefaultBookshelf,
                            hasMore = hasMore,
                            onLoadMore = { 
                                if (!currentIsLoading && !currentIsLoadingMore && currentHasMore && currentBooksSize > 0) {
                                    currentOnLoadMore()
                                }
                            },
                            onBookClick = onBookClick,
                            onMenuAction = onMenuAction
                        )
                    }
                }
            }
        }
    }
}

/**
 * 列表模式内容
 */
@Composable
private fun ListContent(
    bookshelfId: Int,
    books: List<BookWithProgress>,
    isLoadingMore: Boolean,
    isSystemDefaultBookshelf: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onBookClick: (BookWithProgress) -> Unit,
    onMenuAction: (BookWithProgress, BookMenuAction) -> Unit,
) {
    val listState = rememberLazyListState()
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    
    // 监听滚动状态，触发加载更多
    LaunchedEffect(listState, bookshelfId) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            
            lastVisibleItem != null &&
                totalItems > 0 &&
                lastVisibleItem.index >= totalItems - 3
        }
        .distinctUntilChanged()
        .filter { it }
        .collect {
            currentOnLoadMore()
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize()
    ) {
        items(
            items = books,
            key = { it.book.id }
        ) { book ->
            BookshelfBookListItem(
                bookWithProgress = book,
                showMoveToBookshelf = !isSystemDefaultBookshelf,
                onClick = { onBookClick(book) },
                onMenuAction = { action -> onMenuAction(book, action) }
            )
        }
        
        // 加载更多指示器
        if (isLoadingMore) {
            item(key = "loading_more") {
                LoadingMoreIndicator()
            }
        }
        
        // 没有更多数据
        if (!hasMore && books.isNotEmpty() && !isLoadingMore) {
            item(key = "no_more") {
                NoMoreDataIndicator()
            }
        }
    }
}

/**
 * 网格/瀑布流模式内容
 */
@Composable
private fun GridContent(
    bookshelfId: Int,
    books: List<BookWithProgress>,
    isLoadingMore: Boolean,
    isSystemDefaultBookshelf: Boolean,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onBookClick: (BookWithProgress) -> Unit,
    onMenuAction: (BookWithProgress, BookMenuAction) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)
    
    // 监听滚动状态，触发加载更多
    LaunchedEffect(gridState, bookshelfId) {
        snapshotFlow {
            val layoutInfo = gridState.layoutInfo
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val totalItems = layoutInfo.totalItemsCount
            
            lastVisibleItem != null &&
                totalItems > 0 &&
                lastVisibleItem.index >= totalItems - 6
        }
        .distinctUntilChanged()
        .filter { it }
        .collect {
            currentOnLoadMore()
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 120.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = books,
            key = { it.book.id }
        ) { book ->
            BookshelfBookGridItem(
                bookWithProgress = book,
                showMoveToBookshelf = !isSystemDefaultBookshelf,
                onClick = { onBookClick(book) },
                onMenuAction = { action -> onMenuAction(book, action) }
            )
        }
    }
    
    // 加载更多和没有更多数据的指示器放在 Grid 外面处理会更复杂
    // 这里简化处理，不在 Grid 中显示这些指示器
}

/**
 * 空内容提示
 */
@Composable
private fun EmptyContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(Res.string.bookshelf_no_books),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * 加载更多指示器
 */
@Composable
private fun LoadingMoreIndicator() {
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

/**
 * 没有更多数据提示
 */
@Composable
private fun NoMoreDataIndicator() {
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
