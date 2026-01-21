package com.bookd.app.data.vm

import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.data.model.Book
import com.bookd.app.data.model.Bookshelf
import com.bookd.app.data.model.ReadingProgressResponse
import com.bookd.app.data.model.Tag
import com.bookd.app.data.repository.BookRepository
import com.bookd.app.data.repository.BookshelfRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 书籍详情页面状态
 */
data class BookDetailState(
    // 书籍 ID
    val bookId: Int = 0,
    
    // 书籍信息
    val book: Book? = null,
    val tags: List<Tag> = emptyList(),
    val readingProgress: ReadingProgressResponse? = null,
    val bookshelves: List<Bookshelf> = emptyList(),
    val inDefaultBookshelf: Boolean = false,
    
    // 所有用户书架（用于添加到书架对话框）
    val allBookshelves: List<Bookshelf> = emptyList(),
    
    // 加载状态
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    
    // 对话框状态
    val showAddToBookshelfDialog: Boolean = false,
    val isAddingToBookshelf: Boolean = false
) {
    /**
     * 阅读进度百分比 (0-100)
     */
    val readingProgressPercent: Int
        get() = readingProgress?.let { (it.progress * 100).toInt() } ?: 0
    
    /**
     * 是否已收藏（在任何非默认书架中）
     */
    val isFavorite: Boolean
        get() = bookshelves.any { !it.isSystemDefault }
}

/**
 * 用户意图
 */
sealed class BookDetailIntent {
    /** 加载书籍详情 */
    data class LoadBookDetail(val bookId: Int, val forceRefresh: Boolean = false) : BookDetailIntent()
    
    /** 刷新书籍详情 */
    data object Refresh : BookDetailIntent()
    
    /** 显示添加到书架对话框 */
    data object ShowAddToBookshelfDialog : BookDetailIntent()
    
    /** 隐藏添加到书架对话框 */
    data object HideAddToBookshelfDialog : BookDetailIntent()
    
    /** 添加到书架 */
    data class AddToBookshelves(val bookshelfIds: List<Int>) : BookDetailIntent()
    
    /** 从书架移除 */
    data class RemoveFromBookshelf(val bookshelfId: Int) : BookDetailIntent()
    
    /** 切换默认书架状态 */
    data object ToggleDefaultBookshelf : BookDetailIntent()
    
    /** 开始阅读 */
    data object StartReading : BookDetailIntent()
}

/**
 * 一次性效果
 * 
 * 仅用于导航和成功提示，错误由 GlobalExceptionHandler -> AppViewModel -> SnackbarHostScaffold 统一处理
 * 
 * 注意：成功提示使用标识符而非硬编码文本，在 Screen 层使用 stringResource() 获取国际化文本
 */
sealed class BookDetailEffect {
    /** 已添加到书架 */
    data object AddedToBookshelf : BookDetailEffect()
    
    /** 已从书架移除 */
    data object RemovedFromBookshelf : BookDetailEffect()
    
    /** 已添加到默认书架 */
    data object AddedToDefaultBookshelf : BookDetailEffect()
    
    /** 已从默认书架移除 */
    data object RemovedFromDefaultBookshelf : BookDetailEffect()
    
    /** 导航到阅读器 */
    data class NavigateToReader(val bookId: Int) : BookDetailEffect()
    
    /** 返回上一页 */
    data object NavigateBack : BookDetailEffect()
}

/**
 * 书籍详情 ViewModel
 */
class BookDetailViewModel(
    private val bookRepository: BookRepository,
    private val bookshelfRepository: BookshelfRepository
) : BaseViewModel() {
    
    private val _state = MutableStateFlow(BookDetailState())
    val state: StateFlow<BookDetailState> = _state.asStateFlow()
    
    private val _effect = MutableSharedFlow<BookDetailEffect>()
    val effect: SharedFlow<BookDetailEffect> = _effect.asSharedFlow()
    
    /**
     * 处理用户意图
     */
    fun onIntent(intent: BookDetailIntent) {
        when (intent) {
            is BookDetailIntent.LoadBookDetail -> loadBookDetail(intent.bookId, intent.forceRefresh)
            is BookDetailIntent.Refresh -> refresh()
            is BookDetailIntent.ShowAddToBookshelfDialog -> showAddToBookshelfDialog()
            is BookDetailIntent.HideAddToBookshelfDialog -> hideAddToBookshelfDialog()
            is BookDetailIntent.AddToBookshelves -> addToBookshelves(intent.bookshelfIds)
            is BookDetailIntent.RemoveFromBookshelf -> removeFromBookshelf(intent.bookshelfId)
            is BookDetailIntent.ToggleDefaultBookshelf -> toggleDefaultBookshelf()
            is BookDetailIntent.StartReading -> startReading()
        }
    }
    
    private fun loadBookDetail(bookId: Int, forceRefresh: Boolean = false) {
        if (_state.value.isLoading && !forceRefresh) return
        
        scope.launch {
            _state.update { 
                it.copy(
                    bookId = bookId,
                    isLoading = !forceRefresh,
                    isRefreshing = forceRefresh
                )
            }
            
            bookRepository.getBookDetail(bookId).fold(
                onSuccess = { response ->
                    _state.update { 
                        it.copy(
                            book = response.book,
                            tags = response.tags,
                            readingProgress = response.readingProgress,
                            bookshelves = response.bookshelves,
                            inDefaultBookshelf = response.inDefaultBookshelf,
                            isLoading = false,
                            isRefreshing = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { 
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = e.message
                        )
                    }
                    // 抛出异常，由 GlobalExceptionHandler -> AppViewModel 统一处理
                    throw e
                }
            )
        }
    }
    
    private fun refresh() {
        val bookId = _state.value.bookId
        if (bookId > 0) {
            loadBookDetail(bookId, forceRefresh = true)
        }
    }
    
    private fun showAddToBookshelfDialog() {
        scope.launch {
            // 先加载所有书架
            bookshelfRepository.getBookshelves().fold(
                onSuccess = { bookshelves ->
                    _state.update { 
                        it.copy(
                            allBookshelves = bookshelves,
                            showAddToBookshelfDialog = true
                        )
                    }
                },
                onFailure = { e ->
                    // 抛出异常，由 GlobalExceptionHandler -> AppViewModel 统一处理
                    throw e
                }
            )
        }
    }
    
    private fun hideAddToBookshelfDialog() {
        _state.update { it.copy(showAddToBookshelfDialog = false) }
    }
    
    private fun addToBookshelves(bookshelfIds: List<Int>) {
        val bookId = _state.value.bookId
        if (bookId <= 0 || bookshelfIds.isEmpty()) return
        
        scope.launch {
            _state.update { it.copy(isAddingToBookshelf = true) }
            
            bookshelfRepository.addBookToBookshelves(bookId, bookshelfIds).fold(
                onSuccess = {
                    _state.update { 
                        it.copy(
                            isAddingToBookshelf = false,
                            showAddToBookshelfDialog = false
                        )
                    }
                    _effect.emit(BookDetailEffect.AddedToBookshelf)
                    
                    // 刷新书籍详情以更新书架列表
                    refresh()
                },
                onFailure = { e ->
                    _state.update { it.copy(isAddingToBookshelf = false) }
                    // 抛出异常，由 GlobalExceptionHandler -> AppViewModel 统一处理
                    throw e
                }
            )
        }
    }
    
    private fun removeFromBookshelf(bookshelfId: Int) {
        val bookId = _state.value.bookId
        if (bookId <= 0) return
        
        scope.launch {
            bookshelfRepository.removeBookFromBookshelf(bookshelfId, bookId).fold(
                onSuccess = {
                    _effect.emit(BookDetailEffect.RemovedFromBookshelf)
                    // 刷新书籍详情以更新书架列表
                    refresh()
                },
                onFailure = { e ->
                    // 抛出异常，由 GlobalExceptionHandler -> AppViewModel 统一处理
                    throw e
                }
            )
        }
    }
    
    private fun toggleDefaultBookshelf() {
        val bookId = _state.value.bookId
        if (bookId <= 0) return
        
        scope.launch {
            // 找到默认书架
            val defaultBookshelf = _state.value.allBookshelves.find { it.isSystemDefault }
                ?: _state.value.bookshelves.find { it.isSystemDefault }
            
            if (defaultBookshelf == null) {
                // 需要先加载书架列表
                bookshelfRepository.getBookshelves().fold(
                    onSuccess = { bookshelves ->
                        val defaultShelf = bookshelves.find { it.isSystemDefault }
                        if (defaultShelf != null) {
                            toggleBookInDefaultShelf(bookId, defaultShelf.id)
                        }
                    },
                    onFailure = { e ->
                        // 抛出异常，由 GlobalExceptionHandler -> AppViewModel 统一处理
                        throw e
                    }
                )
            } else {
                toggleBookInDefaultShelf(bookId, defaultBookshelf.id)
            }
        }
    }
    
    private suspend fun toggleBookInDefaultShelf(bookId: Int, defaultShelfId: Int) {
        val isInDefault = _state.value.inDefaultBookshelf
        
        if (isInDefault) {
            // 从默认书架移除
            bookshelfRepository.removeBookFromBookshelf(defaultShelfId, bookId).fold(
                onSuccess = {
                    _state.update { it.copy(inDefaultBookshelf = false) }
                    _effect.emit(BookDetailEffect.RemovedFromDefaultBookshelf)
                },
                onFailure = { e ->
                    // 抛出异常，由 GlobalExceptionHandler -> AppViewModel 统一处理
                    throw e
                }
            )
        } else {
            // 添加到默认书架
            bookshelfRepository.addBookToBookshelf(defaultShelfId, bookId).fold(
                onSuccess = {
                    _state.update { it.copy(inDefaultBookshelf = true) }
                    _effect.emit(BookDetailEffect.AddedToDefaultBookshelf)
                },
                onFailure = { e ->
                    // 抛出异常，由 GlobalExceptionHandler -> AppViewModel 统一处理
                    throw e
                }
            )
        }
    }
    
    private fun startReading() {
        val bookId = _state.value.bookId
        if (bookId > 0) {
            scope.launch {
                _effect.emit(BookDetailEffect.NavigateToReader(bookId))
            }
        }
    }
}
