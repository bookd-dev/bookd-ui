package com.bookd.app.screen.bookdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.added_to_bookshelf
import app.composeapp.generated.resources.added_to_default_bookshelf
import app.composeapp.generated.resources.back
import app.composeapp.generated.resources.removed_from_bookshelf
import app.composeapp.generated.resources.removed_from_default_bookshelf
import com.bookd.app.data.model.Book
import com.bookd.app.data.model.Bookshelf
import com.bookd.app.data.model.ReadingProgressResponse
import com.bookd.app.data.model.Tag
import com.bookd.app.data.vm.BookDetailEffect
import com.bookd.app.data.vm.BookDetailIntent
import com.bookd.app.data.vm.BookDetailState
import com.bookd.app.data.vm.BookDetailViewModel
import com.bookd.app.screen.bookdetail.component.AddToBookshelfDialog
import com.bookd.app.screen.bookdetail.content.BookDetailActionSection
import com.bookd.app.screen.bookdetail.content.BookDetailHeaderSection
import com.bookd.app.screen.bookdetail.content.BookDetailInfoSection
import com.bookd.app.screen.bookdetail.content.BookDetailProgressSection
import com.bookd.app.screen.bookdetail.content.BookDetailShelvesSection
import com.bookd.app.screen.bookdetail.content.BookDetailTagsSection
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.screen.RouteReader
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.AppVerticalZHPreview
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookDetailScreen(
    bookId: Int
) {
    val screenContext = rememberScreenContext<BookDetailViewModel>()
    val viewModel = screenContext.viewModel
    val navigator = screenContext.navigator
    val snackbarHostState = screenContext.snackbarHostState
    
    val state by viewModel.state.collectAsState()
    
    // 预加载国际化字符串
    val addedToBookshelfMsg = stringResource(Res.string.added_to_bookshelf)
    val removedFromBookshelfMsg = stringResource(Res.string.removed_from_bookshelf)
    val addedToDefaultBookshelfMsg = stringResource(Res.string.added_to_default_bookshelf)
    val removedFromDefaultBookshelfMsg = stringResource(Res.string.removed_from_default_bookshelf)
    
    // 初始加载
    LaunchedEffect(bookId) {
        viewModel.onIntent(BookDetailIntent.LoadBookDetail(bookId))
    }
    
    // 处理一次性效果
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is BookDetailEffect.AddedToBookshelf -> {
                    snackbarHostState.showSnackbar(addedToBookshelfMsg)
                }
                is BookDetailEffect.RemovedFromBookshelf -> {
                    snackbarHostState.showSnackbar(removedFromBookshelfMsg)
                }
                is BookDetailEffect.AddedToDefaultBookshelf -> {
                    snackbarHostState.showSnackbar(addedToDefaultBookshelfMsg)
                }
                is BookDetailEffect.RemovedFromDefaultBookshelf -> {
                    snackbarHostState.showSnackbar(removedFromDefaultBookshelfMsg)
                }
                is BookDetailEffect.NavigateToReader -> {
                    navigator.navigateTo(RouteReader(effect.bookId))
                }
                is BookDetailEffect.NavigateBack -> {
                    navigator.navigateBack()
                }
            }
        }
    }
    
    BookDetailContent(
        state = state,
        onBackClick = { navigator.navigateBack() },
        onRefresh = { viewModel.onIntent(BookDetailIntent.Refresh) },
        onStartReading = { viewModel.onIntent(BookDetailIntent.StartReading) },
        onToggleDefaultBookshelf = { viewModel.onIntent(BookDetailIntent.ToggleDefaultBookshelf) },
        onShowAddToBookshelfDialog = { viewModel.onIntent(BookDetailIntent.ShowAddToBookshelfDialog) },
        onHideAddToBookshelfDialog = { viewModel.onIntent(BookDetailIntent.HideAddToBookshelfDialog) },
        onAddToBookshelves = { bookshelfIds -> 
            viewModel.onIntent(BookDetailIntent.AddToBookshelves(bookshelfIds)) 
        },
        onRemoveFromBookshelf = { bookshelfId -> 
            viewModel.onIntent(BookDetailIntent.RemoveFromBookshelf(bookshelfId)) 
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BookDetailContent(
    state: BookDetailState,
    onBackClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onStartReading: () -> Unit = {},
    onToggleDefaultBookshelf: () -> Unit = {},
    onShowAddToBookshelfDialog: () -> Unit = {},
    onHideAddToBookshelfDialog: () -> Unit = {},
    onAddToBookshelves: (List<Int>) -> Unit = {},
    onRemoveFromBookshelf: (Int) -> Unit = {}
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val scrollState = rememberScrollState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = state.book?.title ?: "",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // 加载中
                state.isLoading && state.book == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                
                // 加载错误
                state.error != null && state.book == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                // 有数据
                state.book != null -> {
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .padding(16.dp)
                        ) {
                            // 书籍头部信息（封面、标题、作者）
                            BookDetailHeaderSection(book = state.book)
                            
                            // 阅读进度
                            if (state.readingProgress != null) {
                                BookDetailProgressSection(
                                    progress = state.readingProgress,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            
                            // 操作按钮
                            BookDetailActionSection(
                                inDefaultBookshelf = state.inDefaultBookshelf,
                                onStartReading = onStartReading,
                                onToggleDefaultBookshelf = onToggleDefaultBookshelf,
                                onAddToBookshelf = onShowAddToBookshelfDialog,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                            
                            // 标签
                            if (state.tags.isNotEmpty()) {
                                BookDetailTagsSection(
                                    tags = state.tags,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            
                            // 所在书架
                            if (state.bookshelves.isNotEmpty()) {
                                BookDetailShelvesSection(
                                    bookshelves = state.bookshelves,
                                    onRemoveFromBookshelf = onRemoveFromBookshelf,
                                    modifier = Modifier.padding(top = 16.dp)
                                )
                            }
                            
                            // 书籍详细信息
                            BookDetailInfoSection(
                                book = state.book,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    // 添加到书架对话框
    if (state.showAddToBookshelfDialog) {
        AddToBookshelfDialog(
            allBookshelves = state.allBookshelves,
            currentBookshelves = state.bookshelves,
            isLoading = state.isAddingToBookshelf,
            onDismiss = onHideAddToBookshelfDialog,
            onConfirm = onAddToBookshelves
        )
    }
}

@AppVerticalZHPreview
@Composable
private fun BookDetailContentPreview() {
    AppPreviewContent {
        BookDetailContent(
            state = BookDetailState(
                bookId = 1,
                book = Book(
                    id = 1,
                    title = "测试书籍标题",
                    author = "测试作者",
                    format = "epub",
                    filePath = "/path/to/book.epub",
                    fileSize = 1024000,
                    coverPath = null,
                    isbn = "978-7-111-11111-1",
                    publisher = "测试出版社",
                    description = "这是一本测试书籍的描述，用于预览书籍详情页面的效果。",
                    sourceId = 1,
                    chapterCount = 20,
                    totalWordCount = 100000,
                    totalImageCount = 50,
                    chaptersParsed = true,
                    chaptersCount = 20,
                    lastParsedAt = "2024-01-01T00:00:00Z",
                    parseStatus = "completed",
                    parseProgress = 100,
                    createdAt = "2024-01-01T00:00:00Z",
                    updatedAt = "2024-01-01T00:00:00Z"
                ),
                tags = listOf(
                    Tag(1, "小说"),
                    Tag(2, "科幻"),
                    Tag(3, "经典")
                ),
                readingProgress = ReadingProgressResponse(
                    id = 1,
                    bookId = 1,
                    progress = 0.35,
                    currentPage = 35,
                    totalPages = 100,
                    cfiLocation = null,
                    documentId = null,
                    deviceId = null,
                    lastReadAt = "2024-01-15T10:30:00Z"
                ),
                bookshelves = listOf(
                    Bookshelf(
                        id = 1,
                        userId = 1,
                        name = "我的书架",
                        description = null,
                        sortOrder = 0,
                        bookCount = 10,
                        isSystemDefault = true,
                        createdAt = "2024-01-01T00:00:00Z",
                        updatedAt = "2024-01-01T00:00:00Z"
                    ),
                    Bookshelf(
                        id = 2,
                        userId = 1,
                        name = "科幻小说",
                        description = "收藏的科幻小说",
                        sortOrder = 1,
                        bookCount = 5,
                        isSystemDefault = false,
                        createdAt = "2024-01-01T00:00:00Z",
                        updatedAt = "2024-01-01T00:00:00Z"
                    )
                ),
                inDefaultBookshelf = true
            )
        )
    }
}
