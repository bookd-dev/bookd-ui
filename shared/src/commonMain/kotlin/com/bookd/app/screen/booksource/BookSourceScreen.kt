package com.bookd.app.screen.booksource

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.bookd.app.data.model.Book
import com.bookd.app.data.model.BookSource
import com.bookd.app.data.vm.BookSourceEffect
import com.bookd.app.data.vm.BookSourceIntent
import com.bookd.app.data.vm.BookSourceState
import com.bookd.app.data.vm.BookSourceViewModel
import com.bookd.app.screen.RouteBookDetail
import com.bookd.app.screen.RouteSearchBook
import com.bookd.app.screen.booksource.content.BookSourceHeaderContent
import com.bookd.app.screen.booksource.content.BookSourceListContent
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.AppVerticalZHPreview
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.no_book_source
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookSourceScreen() {
    val screenContext = rememberScreenContext<BookSourceViewModel>()
    val viewModel = screenContext.viewModel
    val state by viewModel.state.collectAsState()
    
    // 初始加载
    LaunchedEffect(Unit) {
        viewModel.onIntent(BookSourceIntent.LoadSources)
    }
    
    // 处理一次性效果
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is BookSourceEffect.ScrollToTop -> {
                    // 滚动到顶部
                    val scrollState = viewModel.getScrollState(
                        state.sources.indexOfFirst { it.id == effect.sourceId }
                    )
                    scrollState.animateScrollToItem(0)
                }
            }
        }
    }

    BookSourceContent(
        state = state,
        tabRowOffset = viewModel.tabRowOffset,
        onSourceSelected = { index ->
            viewModel.onIntent(BookSourceIntent.SelectSource(index))
        },
        onTabRowOffsetChanged = { viewModel.tabRowOffset = it },
        onGetScrollState = { page -> viewModel.getScrollState(page) },
        onLoadMore = { sourceId ->
            viewModel.onIntent(BookSourceIntent.LoadMoreBooks(sourceId))
        },
        onRefresh = {
            viewModel.onIntent(BookSourceIntent.RefreshCurrentBooks)
        },
        onRefreshAll = {
            viewModel.onIntent(BookSourceIntent.RefreshAll)
        },
        onBookClick = { book ->
            screenContext.navigator.navigateTo(RouteBookDetail(bookId = book.id))
        },
        onSearchClick = {
            screenContext.navigator.navigateTo(RouteSearchBook)
        }
    )
}

@Composable
private fun BookSourceContent(
    state: BookSourceState,
    tabRowOffset: Float = 0f,
    onSourceSelected: (Int) -> Unit = {},
    onTabRowOffsetChanged: (Float) -> Unit = {},
    onGetScrollState: (Int) -> LazyListState = { LazyListState() },
    onLoadMore: (Int) -> Unit = {},
    onRefresh: () -> Unit = {},
    onRefreshAll: () -> Unit = {},
    onBookClick: (Book) -> Unit = {},
    onSearchClick: () -> Unit = {},
) {
    val sources = state.sources
    val coroutineScope = rememberCoroutineScope()
    
    // PagerState 需要根据 sources 数量动态创建
    val pagerState = rememberPagerState(
        initialPage = state.selectedSourceIndex.coerceIn(0, maxOf(sources.size - 1, 0))
    ) { 
        maxOf(sources.size, 1) 
    }
    
    // 同步 pagerState 到外部
    LaunchedEffect(pagerState.currentPage) {
        if (sources.isNotEmpty() && pagerState.currentPage != state.selectedSourceIndex) {
            onSourceSelected(pagerState.currentPage)
        }
    }
    
    // 当 selectedSourceIndex 从外部变化时，同步到 pagerState
    LaunchedEffect(state.selectedSourceIndex) {
        if (sources.isNotEmpty() && pagerState.currentPage != state.selectedSourceIndex) {
            pagerState.scrollToPage(state.selectedSourceIndex)
        }
    }

    // Tab 行高度
    val tabRowHeightPx = with(LocalDensity.current) { 48.dp.toPx() }
    // 本地维护 offset 用于 UI，同步到外部
    var localTabRowOffset by remember { mutableFloatStateOf(tabRowOffset) }
    // 折叠状态由 offset 派生
    val isCollapsed by remember { derivedStateOf { localTabRowOffset <= -tabRowHeightPx * 0.5f } }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header - 包含数据源 Tabs 和菜单
        BookSourceHeaderContent(
            sources = sources,
            pagerState = pagerState,
            isCollapsed = isCollapsed,
            isRefreshingAll = state.sourcesLoading ||
                state.booksLoading.values.any { it } ||
                state.booksLoadingMore.values.any { it } ||
                state.booksRefreshing.values.any { it },
            onBookSourceChange = { index ->
                coroutineScope.launch {
                    pagerState.scrollToPage(index)
                }
            },
            onRefreshAllClick = onRefreshAll,
            onSearchClick = onSearchClick
        )

        // 主体内容
        when {
            // 书源加载中
            state.sourcesLoading && sources.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // 书源加载错误
            state.sourcesError != null && sources.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.sourcesError,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // 无书源
            sources.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.no_book_source),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 有书源，显示 Pager
            else -> {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val source = sources.getOrNull(page)
                    if (source != null) {
                        val books = state.booksBySource[source.id] ?: emptyList()
                        val isLoading = state.booksLoading[source.id] ?: false
                        val isLoadingMore = state.booksLoadingMore[source.id] ?: false
                        val isRefreshing = state.booksRefreshing[source.id] ?: false
                        val hasMore = state.booksHasMore[source.id] ?: true
                        val error = state.booksError[source.id]
                        
                        BookSourceListContent(
                            sourceId = source.id,
                            books = books,
                            isLoading = isLoading,
                            isLoadingMore = isLoadingMore,
                            isRefreshing = isRefreshing,
                            hasMore = hasMore,
                            error = error,
                            scrollState = onGetScrollState(page),
                            tabRowHeightPx = tabRowHeightPx,
                            tabRowOffset = localTabRowOffset,
                            onTabRowOffsetChanged = {
                                localTabRowOffset = it
                                onTabRowOffsetChanged(it)
                            },
                            onLoadMore = { onLoadMore(source.id) },
                            onRefresh = onRefresh,
                            onBookClick = onBookClick
                        )
                    }
                }
            }
        }
    }
}

@AppVerticalZHPreview
@Composable
private fun BookSourceScreenPreview() {
    AppPreviewContent {
        BookSourceContent(
            state = BookSourceState(
                sources = listOf(
                    BookSource(1, "本地书源", "/path/to/local", true),
                    BookSource(2, "网络书源", "/path/to/remote", true),
                ),
                selectedSourceIndex = 0
            )
        )
    }
}
