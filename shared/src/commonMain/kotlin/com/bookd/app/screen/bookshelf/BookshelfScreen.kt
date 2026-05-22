package com.bookd.app.screen.bookshelf

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.book_added_to_bookshelves
import app.composeapp.generated.resources.book_moved_to_bookshelf
import app.composeapp.generated.resources.book_removed_from_all
import app.composeapp.generated.resources.bookshelf_created
import app.composeapp.generated.resources.bookshelf_deleted
import app.composeapp.generated.resources.bookshelf_empty
import app.composeapp.generated.resources.bookshelf_updated
import app.composeapp.generated.resources.book_removed
import com.bookd.app.data.model.Bookshelf
import com.bookd.app.data.model.BookWithProgress
import com.bookd.app.data.structure.BookshelfMenu
import com.bookd.app.data.vm.BookshelfEffect
import com.bookd.app.data.vm.BookshelfIntent
import com.bookd.app.data.vm.BookshelfState
import com.bookd.app.data.vm.BookshelfViewModel
import com.bookd.app.screen.RouteBookDetail
import com.bookd.app.screen.RouteNetworkConfig
import com.bookd.app.screen.RouteReader
import com.bookd.app.screen.bookshelf.component.AddToBookshelvesDialog
import com.bookd.app.screen.bookshelf.component.BookMenuAction
import com.bookd.app.screen.bookshelf.component.CreateBookshelfDialog
import com.bookd.app.screen.bookshelf.component.DeleteBookshelfDialog
import com.bookd.app.screen.bookshelf.component.EditBookshelfDialog
import com.bookd.app.screen.bookshelf.component.MoveToBookshelfDialog
import com.bookd.app.screen.bookshelf.component.RemoveFromAllDialog
import com.bookd.app.screen.bookshelf.content.BookshelfHeaderContent
import com.bookd.app.screen.bookshelf.content.BookshelfListContent
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.AppVerticalZHPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun BookshelfScreen() {
    val screenContext = rememberScreenContext<BookshelfViewModel>()
    val viewModel = screenContext.viewModel
    val state by viewModel.state.collectAsState()
    val navigator = screenContext.navigator
    
    // 成功消息字符串
    val bookshelfCreatedMsg = stringResource(Res.string.bookshelf_created)
    val bookshelfUpdatedMsg = stringResource(Res.string.bookshelf_updated)
    val bookshelfDeletedMsg = stringResource(Res.string.bookshelf_deleted)
    val bookRemovedMsg = stringResource(Res.string.book_removed)
    val bookAddedToBookshelvesMsg = stringResource(Res.string.book_added_to_bookshelves)
    val bookMovedToBookshelfMsg = stringResource(Res.string.book_moved_to_bookshelf)
    val bookRemovedFromAllMsg = stringResource(Res.string.book_removed_from_all)
    
    // 记录上一次的导航栈大小，用于检测返回
    var lastBackStackSize by remember { mutableIntStateOf(navigator.backStackSize.value) }
    
    // 初始加载
    LaunchedEffect(Unit) {
        viewModel.onIntent(BookshelfIntent.Initialize)
    }
    
    // 监听导航栈变化，检测返回操作并静默刷新
    LaunchedEffect(Unit) {
        navigator.backStackSize
            .drop(1) // 跳过初始值
            .collect { newSize ->
                // 如果栈变小了，说明有页面被弹出（返回操作）
                if (newSize < lastBackStackSize && state.bookshelves.isNotEmpty()) {
                    viewModel.onIntent(BookshelfIntent.SilentRefresh)
                }
                lastBackStackSize = newSize
            }
    }
    
    // 处理一次性效果
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is BookshelfEffect.NavigateToBookDetail -> {
                    screenContext.navigator.navigateTo(RouteBookDetail(bookId = effect.bookId))
                }
                is BookshelfEffect.NavigateToReader -> {
                    screenContext.navigator.navigateTo(RouteReader(effect.bookId))
                }
                is BookshelfEffect.BookshelfCreated -> {
                    screenContext.snackbarHostState.showSnackbar(bookshelfCreatedMsg)
                }
                is BookshelfEffect.BookshelfUpdated -> {
                    screenContext.snackbarHostState.showSnackbar(bookshelfUpdatedMsg)
                }
                is BookshelfEffect.BookshelfDeleted -> {
                    screenContext.snackbarHostState.showSnackbar(bookshelfDeletedMsg)
                }
                is BookshelfEffect.BookRemoved -> {
                    screenContext.snackbarHostState.showSnackbar(bookRemovedMsg)
                }
                is BookshelfEffect.BookAddedToBookshelves -> {
                    screenContext.snackbarHostState.showSnackbar(bookAddedToBookshelvesMsg)
                }
                is BookshelfEffect.BookMovedToBookshelf -> {
                    screenContext.snackbarHostState.showSnackbar(bookMovedToBookshelfMsg)
                }
                is BookshelfEffect.BookRemovedFromAll -> {
                    screenContext.snackbarHostState.showSnackbar(bookRemovedFromAllMsg)
                }
            }
        }
    }

    BookshelfContent(
        state = state,
        onIntent = { viewModel.onIntent(it) },
        onMenuClick = { menu ->
            when (menu) {
                BookshelfMenu.NetworkConfig -> screenContext.navigator.navigateUnconditionally(RouteNetworkConfig)
                BookshelfMenu.ToggleViewMode -> viewModel.onIntent(BookshelfIntent.ToggleViewMode)
                BookshelfMenu.AddBookshelf -> viewModel.onIntent(BookshelfIntent.ShowCreateDialog)
            }
        },
        onBookClick = { book ->
            // 点击书籍直接开始阅读
            viewModel.onIntent(BookshelfIntent.StartReading(book.book.id))
        },
        onMenuAction = { book, action ->
            when (action) {
                BookMenuAction.Detail -> {
                    viewModel.onIntent(BookshelfIntent.OpenBookDetail(book.book.id))
                }
                BookMenuAction.AddToBookshelves -> {
                    viewModel.onIntent(BookshelfIntent.ShowAddToBookshelvesDialog(book))
                }
                BookMenuAction.MoveToBookshelf -> {
                    viewModel.onIntent(BookshelfIntent.ShowMoveToBookshelfDialog(book))
                }
                BookMenuAction.RemoveFromAll -> {
                    viewModel.onIntent(BookshelfIntent.ShowRemoveFromAllDialog(book))
                }
            }
        },
        onEditBookshelf = { bookshelf ->
            viewModel.onIntent(BookshelfIntent.ShowEditDialog(bookshelf))
        },
        onDeleteBookshelf = { bookshelf ->
            viewModel.onIntent(BookshelfIntent.ShowDeleteDialog(bookshelf))
        }
    )
}

@Composable
private fun BookshelfContent(
    state: BookshelfState,
    onIntent: (BookshelfIntent) -> Unit = {},
    onMenuClick: (BookshelfMenu) -> Unit = {},
    onBookClick: (BookWithProgress) -> Unit = {},
    onMenuAction: (BookWithProgress, BookMenuAction) -> Unit = { _, _ -> },
    onEditBookshelf: (Bookshelf) -> Unit = {},
    onDeleteBookshelf: (Bookshelf) -> Unit = {},
) {
    val bookshelves = state.bookshelves
    val coroutineScope = rememberCoroutineScope()
    
    // PagerState 需要根据 bookshelves 数量动态创建
    val pagerState = rememberPagerState(
        initialPage = bookshelves.indexOfFirst { it.id == state.selectedBookshelfId }.coerceAtLeast(0)
    ) { 
        maxOf(bookshelves.size, 1) 
    }
    
    // 同步 pagerState 到外部
    LaunchedEffect(pagerState.currentPage) {
        if (bookshelves.isNotEmpty() && pagerState.currentPage < bookshelves.size) {
            val bookshelf = bookshelves[pagerState.currentPage]
            if (bookshelf.id != state.selectedBookshelfId) {
                onIntent(BookshelfIntent.SelectBookshelf(bookshelf.id))
            }
        }
    }
    
    // 当 selectedBookshelfId 从外部变化时，同步到 pagerState
    LaunchedEffect(state.selectedBookshelfId) {
        if (bookshelves.isNotEmpty()) {
            val targetIndex = bookshelves.indexOfFirst { it.id == state.selectedBookshelfId }
            if (targetIndex >= 0 && pagerState.currentPage != targetIndex) {
                pagerState.scrollToPage(targetIndex)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header - 包含书架 Tabs 和菜单
        BookshelfHeaderContent(
            bookshelves = bookshelves,
            pagerState = pagerState,
            isGridMode = state.isGridMode,
            onBookshelfChange = { index ->
                coroutineScope.launch {
                    pagerState.scrollToPage(index)
                }
            },
            onMenuClick = onMenuClick,
            onEditBookshelf = onEditBookshelf,
            onDeleteBookshelf = onDeleteBookshelf
        )

        // 主体内容
        when {
            // 书架加载中
            state.isLoadingBookshelves && bookshelves.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // 书架加载错误
            state.error != null && bookshelves.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            // 无书架
            bookshelves.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.bookshelf_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // 有书架，显示 Pager
            else -> {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val bookshelf = bookshelves.getOrNull(page)
                    if (bookshelf != null) {
                        val books = state.booksByBookshelf[bookshelf.id] ?: emptyList()
                        val isLoading = state.loadingBooksForBookshelf == bookshelf.id
                        val isLoadingMore = state.loadingMoreForBookshelf == bookshelf.id
                        val hasMore = state.hasMoreByBookshelf[bookshelf.id] ?: false
                        
                        BookshelfListContent(
                            bookshelfId = bookshelf.id,
                            books = books,
                            isLoading = isLoading,
                            isLoadingMore = isLoadingMore,
                            isRefreshing = state.isRefreshing,
                            isGridMode = state.isGridMode,
                            isSystemDefaultBookshelf = bookshelf.isSystemDefault,
                            hasMore = hasMore,
                            onLoadMore = { onIntent(BookshelfIntent.LoadMoreBooks(bookshelf.id)) },
                            onRefresh = { onIntent(BookshelfIntent.Refresh) },
                            onBookClick = onBookClick,
                            onMenuAction = onMenuAction
                        )
                    }
                }
            }
        }
    }
    
    // 对话框
    
    // 创建书架对话框
    if (state.showCreateDialog) {
        CreateBookshelfDialog(
            isLoading = state.isDialogLoading,
            onDismiss = { onIntent(BookshelfIntent.HideCreateDialog) },
            onConfirm = { name, description ->
                onIntent(BookshelfIntent.CreateBookshelf(name, description))
            }
        )
    }
    
    // 编辑书架对话框
    state.manageDialog.editingBookshelf?.let { bookshelf ->
        if (state.showEditDialog) {
            EditBookshelfDialog(
                bookshelf = bookshelf,
                isLoading = state.isDialogLoading,
                onDismiss = { onIntent(BookshelfIntent.HideEditDialog) },
                onConfirm = { id, name, description ->
                    onIntent(BookshelfIntent.UpdateBookshelf(id, name, description))
                }
            )
        }
    }
    
    // 删除书架确认对话框
    state.manageDialog.editingBookshelf?.let { bookshelf ->
        if (state.showDeleteDialog) {
            DeleteBookshelfDialog(
                bookshelf = bookshelf,
                isLoading = state.isDialogLoading,
                onDismiss = { onIntent(BookshelfIntent.HideDeleteDialog) },
                onConfirm = { id ->
                    onIntent(BookshelfIntent.DeleteBookshelf(id))
                }
            )
        }
    }
    
    // 添加到书架对话框
    state.addToBookshelvesDialog.book?.let { book ->
        if (state.showAddToBookshelvesDialog) {
            AddToBookshelvesDialog(
                book = book,
                availableBookshelves = state.addToBookshelvesAvailable,
                selectedBookshelves = state.addToBookshelvesSelected,
                isLoading = state.isLoadingAddToBookshelves,
                isUpdating = state.isAddingToBookshelves,
                onToggleBookshelf = { bookshelfId ->
                    onIntent(BookshelfIntent.ToggleAddToBookshelfSelection(bookshelfId))
                },
                onDismiss = { onIntent(BookshelfIntent.HideAddToBookshelvesDialog) },
                onConfirm = { bookId ->
                    onIntent(BookshelfIntent.ConfirmAddToBookshelves(bookId))
                }
            )
        }
    }
    
    // 移动到书架对话框
    state.moveToBookshelfDialog.book?.let { book ->
        if (state.showMoveToBookshelfDialog) {
            MoveToBookshelfDialog(
                book = book,
                availableBookshelves = state.moveToBookshelfAvailable,
                selectedBookshelf = state.moveToBookshelfSelected,
                isLoading = state.isLoadingMoveToBookshelf,
                isUpdating = state.isMovingToBookshelf,
                onSelectBookshelf = { bookshelfId ->
                    onIntent(BookshelfIntent.SelectMoveToBookshelf(bookshelfId))
                },
                onDismiss = { onIntent(BookshelfIntent.HideMoveToBookshelfDialog) },
                onConfirm = { bookId ->
                    onIntent(BookshelfIntent.ConfirmMoveToBookshelf(bookId))
                }
            )
        }
    }
    
    // 从所有书架移除确认对话框
    state.removeFromAllDialog.book?.let { book ->
        if (state.showRemoveFromAllDialog) {
            RemoveFromAllDialog(
                book = book,
                isRemoving = state.isRemovingFromAll,
                onDismiss = { onIntent(BookshelfIntent.HideRemoveFromAllDialog) },
                onConfirm = { bookId ->
                    onIntent(BookshelfIntent.ConfirmRemoveFromAll(bookId))
                }
            )
        }
    }
}

@AppVerticalZHPreview
@Composable
private fun BookshelfScreenPreview() {
    AppPreviewContent {
        BookshelfContent(
            state = BookshelfState(
                bookshelves = listOf(
                    Bookshelf(
                        id = 1,
                        userId = 1,
                        name = "默认书架",
                        description = null,
                        sortOrder = 0,
                        bookCount = 10,
                        isSystemDefault = true,
                        createdAt = "",
                        updatedAt = ""
                    ),
                    Bookshelf(
                        id = 2,
                        userId = 1,
                        name = "收藏",
                        description = null,
                        sortOrder = 1,
                        bookCount = 5,
                        isSystemDefault = false,
                        createdAt = "",
                        updatedAt = ""
                    ),
                ),
                selectedBookshelfId = 1
            )
        )
    }
}
