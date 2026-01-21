package com.bookd.app.data.vm

import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.data.model.Bookshelf
import com.bookd.app.data.model.BookWithProgress
import com.bookd.app.data.repository.BookshelfPreferenceRepository
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
 * 书架页面状态
 */
data class BookshelfState(
    // 书架列表
    val bookshelves: List<Bookshelf> = emptyList(),
    
    // 当前选中的书架 ID（Tab）
    val selectedBookshelfId: Int? = null,
    
    // 每个书架的书籍列表缓存 (bookshelfId -> books)
    val booksByBookshelf: Map<Int, List<BookWithProgress>> = emptyMap(),
    
    // 每个书架的总书籍数 (bookshelfId -> total)
    val totalByBookshelf: Map<Int, Int> = emptyMap(),
    
    // 每个书架是否还有更多数据 (bookshelfId -> hasMore)
    val hasMoreByBookshelf: Map<Int, Boolean> = emptyMap(),
    
    // 折叠状态（用于列表模式）(bookshelfId -> isCollapsed)
    val collapsedBookshelves: Set<Int> = emptySet(),
    
    // 显示模式: "list" 或 "grid"
    val viewMode: String = BookshelfPreferenceRepository.VIEW_MODE_LIST,
    
    // 加载状态
    val isLoadingBookshelves: Boolean = false,
    val isRefreshing: Boolean = false,
    val loadingBooksForBookshelf: Int? = null, // 正在加载书籍的书架 ID
    val loadingMoreForBookshelf: Int? = null,  // 正在加载更多的书架 ID
    
    // 对话框状态
    val showCreateDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val editingBookshelf: Bookshelf? = null,
    val isDialogLoading: Boolean = false,
    
    // 添加到书架对话框状态（只显示书籍未加入的书架）
    val showAddToBookshelvesDialog: Boolean = false,
    val addToBookshelvesBook: BookWithProgress? = null,
    val addToBookshelvesAvailable: List<Bookshelf> = emptyList(),  // 可添加的书架（书籍未加入的）
    val addToBookshelvesSelected: Set<Int> = emptySet(),           // 用户选中的书架 ID
    val isLoadingAddToBookshelves: Boolean = false,
    val isAddingToBookshelves: Boolean = false,
    
    // 移动到书架对话框状态（单选，从当前书架移动到目标书架）
    val showMoveToBookshelfDialog: Boolean = false,
    val moveToBookshelfBook: BookWithProgress? = null,
    val moveToBookshelfAvailable: List<Bookshelf> = emptyList(),  // 可移动到的书架（排除当前书架和系统默认）
    val moveToBookshelfSelected: Int? = null,                      // 选中的目标书架ID（单选）
    val isLoadingMoveToBookshelf: Boolean = false,
    val isMovingToBookshelf: Boolean = false,
    
    // 从所有书架移除确认对话框状态
    val showRemoveFromAllDialog: Boolean = false,
    val removeFromAllBook: BookWithProgress? = null,
    val isRemovingFromAll: Boolean = false,
    
    // 错误状态
    val error: String? = null
) {
    /**
     * 是否为列表模式
     */
    val isListMode: Boolean
        get() = viewMode == BookshelfPreferenceRepository.VIEW_MODE_LIST
    
    /**
     * 是否为瀑布流模式
     */
    val isGridMode: Boolean
        get() = viewMode == BookshelfPreferenceRepository.VIEW_MODE_GRID
    
    /**
     * 当前选中书架
     */
    val selectedBookshelf: Bookshelf?
        get() = bookshelves.find { it.id == selectedBookshelfId }
    
    /**
     * 当前书架的书籍列表
     */
    val currentBooks: List<BookWithProgress>
        get() = selectedBookshelfId?.let { booksByBookshelf[it] } ?: emptyList()
    
    /**
     * 当前书架是否有更多数据
     */
    val currentHasMore: Boolean
        get() = selectedBookshelfId?.let { hasMoreByBookshelf[it] } ?: false
    
    /**
     * 当前书架的总书籍数
     */
    val currentTotal: Int
        get() = selectedBookshelfId?.let { totalByBookshelf[it] } ?: 0
    
    /**
     * 是否正在加载当前书架的书籍
     */
    val isLoadingCurrentBooks: Boolean
        get() = loadingBooksForBookshelf == selectedBookshelfId
    
    /**
     * 是否正在加载更多
     */
    val isLoadingMore: Boolean
        get() = loadingMoreForBookshelf == selectedBookshelfId
    
    /**
     * 判断某个书架是否折叠
     */
    fun isBookshelfCollapsed(bookshelfId: Int): Boolean = bookshelfId in collapsedBookshelves
    
    /**
     * 获取指定书架的书籍列表
     */
    fun getBooksForBookshelf(bookshelfId: Int): List<BookWithProgress> = 
        booksByBookshelf[bookshelfId] ?: emptyList()
    
    /**
     * 获取指定书架的总书籍数
     */
    fun getTotalForBookshelf(bookshelfId: Int): Int = 
        totalByBookshelf[bookshelfId] ?: 0
}

/**
 * 用户意图
 */
sealed class BookshelfIntent {
    /** 初始化加载 */
    data object Initialize : BookshelfIntent()
    
    /** 刷新所有数据（显示刷新 UI） */
    data object Refresh : BookshelfIntent()
    
    /** 静默刷新（不显示刷新 UI，用于自动刷新） */
    data object SilentRefresh : BookshelfIntent()
    
    /** 切换选中的书架（Tab 切换） */
    data class SelectBookshelf(val bookshelfId: Int) : BookshelfIntent()
    
    /** 加载书架书籍 */
    data class LoadBooks(val bookshelfId: Int, val forceRefresh: Boolean = false) : BookshelfIntent()
    
    /** 加载更多书籍 */
    data class LoadMoreBooks(val bookshelfId: Int) : BookshelfIntent()
    
    /** 切换书架折叠状态 */
    data class ToggleCollapse(val bookshelfId: Int) : BookshelfIntent()
    
    /** 切换显示模式 */
    data object ToggleViewMode : BookshelfIntent()
    
    /** 显示创建书架对话框 */
    data object ShowCreateDialog : BookshelfIntent()
    
    /** 隐藏创建书架对话框 */
    data object HideCreateDialog : BookshelfIntent()
    
    /** 创建新书架 */
    data class CreateBookshelf(val name: String, val description: String?) : BookshelfIntent()
    
    /** 显示编辑书架对话框 */
    data class ShowEditDialog(val bookshelf: Bookshelf) : BookshelfIntent()
    
    /** 隐藏编辑书架对话框 */
    data object HideEditDialog : BookshelfIntent()
    
    /** 更新书架 */
    data class UpdateBookshelf(val id: Int, val name: String?, val description: String?) : BookshelfIntent()
    
    /** 显示删除确认对话框 */
    data class ShowDeleteDialog(val bookshelf: Bookshelf) : BookshelfIntent()
    
    /** 隐藏删除确认对话框 */
    data object HideDeleteDialog : BookshelfIntent()
    
    /** 删除书架 */
    data class DeleteBookshelf(val id: Int) : BookshelfIntent()
    
    /** 从书架移除书籍 */
    data class RemoveBookFromBookshelf(val bookshelfId: Int, val bookId: Int) : BookshelfIntent()
    
    /** 打开书籍详情 */
    data class OpenBookDetail(val bookId: Int) : BookshelfIntent()
    
    /** 开始阅读 */
    data class StartReading(val bookId: Int) : BookshelfIntent()
    
    // ==================== 添加到书架对话框 ====================
    
    /** 显示添加到书架对话框（只显示书籍未加入的书架） */
    data class ShowAddToBookshelvesDialog(val book: BookWithProgress) : BookshelfIntent()
    
    /** 隐藏添加到书架对话框 */
    data object HideAddToBookshelvesDialog : BookshelfIntent()
    
    /** 切换添加到书架对话框中的书架选中状态 */
    data class ToggleAddToBookshelfSelection(val bookshelfId: Int) : BookshelfIntent()
    
    /** 确认添加到选中的书架 */
    data class ConfirmAddToBookshelves(val bookId: Int) : BookshelfIntent()
    
    // ==================== 移动到书架对话框 ====================
    
    /** 显示移动到书架对话框（单选目标书架） */
    data class ShowMoveToBookshelfDialog(val book: BookWithProgress) : BookshelfIntent()
    
    /** 隐藏移动到书架对话框 */
    data object HideMoveToBookshelfDialog : BookshelfIntent()
    
    /** 选择目标书架（单选） */
    data class SelectMoveToBookshelf(val bookshelfId: Int) : BookshelfIntent()
    
    /** 确认移动到书架 */
    data class ConfirmMoveToBookshelf(val bookId: Int) : BookshelfIntent()
    
    // ==================== 从所有书架移除对话框 ====================
    
    /** 显示从所有书架移除确认对话框 */
    data class ShowRemoveFromAllDialog(val book: BookWithProgress) : BookshelfIntent()
    
    /** 隐藏从所有书架移除确认对话框 */
    data object HideRemoveFromAllDialog : BookshelfIntent()
    
    /** 确认从所有书架移除 */
    data class ConfirmRemoveFromAll(val bookId: Int) : BookshelfIntent()
}

/**
 * 一次性效果
 * 
 * 仅用于导航和成功提示，错误由 GlobalExceptionHandler -> AppViewModel -> SnackbarHostScaffold 统一处理
 */
sealed class BookshelfEffect {
    /** 导航到书籍详情 */
    data class NavigateToBookDetail(val bookId: Int) : BookshelfEffect()
    
    /** 导航到阅读器 */
    data class NavigateToReader(val bookId: Int) : BookshelfEffect()
    
    /** 显示成功消息 - 仅用于操作成功后的反馈 */
    data object BookshelfCreated : BookshelfEffect()
    
    /** 书架更新成功 */
    data object BookshelfUpdated : BookshelfEffect()
    
    /** 书架删除成功 */
    data object BookshelfDeleted : BookshelfEffect()
    
    /** 书籍移除成功（从单个书架） */
    data object BookRemoved : BookshelfEffect()
    
    /** 书籍已添加到书架 */
    data object BookAddedToBookshelves : BookshelfEffect()
    
    /** 书籍已移动到书架 */
    data object BookMovedToBookshelf : BookshelfEffect()
    
    /** 书籍已从所有书架移除 */
    data object BookRemovedFromAll : BookshelfEffect()
}

/**
 * 书架 ViewModel
 * 
 * 管理书架页面的状态和业务逻辑
 * 策略：
 * - 本地缓存优先，后台同步网络数据
 * - Tab 切换时按需加载书籍
 * - 支持分页加载
 */
class BookshelfViewModel(
    private val bookshelfRepository: BookshelfRepository,
    private val preferenceRepository: BookshelfPreferenceRepository
) : BaseViewModel() {
    
    private val _state = MutableStateFlow(BookshelfState())
    val state: StateFlow<BookshelfState> = _state.asStateFlow()
    
    private val _effect = MutableSharedFlow<BookshelfEffect>()
    val effect: SharedFlow<BookshelfEffect> = _effect.asSharedFlow()
    
    init {
        // 初始化时加载偏好设置
        _state.update { it.copy(viewMode = preferenceRepository.viewMode) }
    }
    
    /**
     * 处理用户意图
     */
    fun onIntent(intent: BookshelfIntent) {
        when (intent) {
            is BookshelfIntent.Initialize -> initialize()
            is BookshelfIntent.Refresh -> refresh(showRefreshingUI = true)
            is BookshelfIntent.SilentRefresh -> refresh(showRefreshingUI = false)
            is BookshelfIntent.SelectBookshelf -> selectBookshelf(intent.bookshelfId)
            is BookshelfIntent.LoadBooks -> loadBooks(intent.bookshelfId, intent.forceRefresh)
            is BookshelfIntent.LoadMoreBooks -> loadMoreBooks(intent.bookshelfId)
            is BookshelfIntent.ToggleCollapse -> toggleCollapse(intent.bookshelfId)
            is BookshelfIntent.ToggleViewMode -> toggleViewMode()
            is BookshelfIntent.ShowCreateDialog -> showCreateDialog()
            is BookshelfIntent.HideCreateDialog -> hideCreateDialog()
            is BookshelfIntent.CreateBookshelf -> createBookshelf(intent.name, intent.description)
            is BookshelfIntent.ShowEditDialog -> showEditDialog(intent.bookshelf)
            is BookshelfIntent.HideEditDialog -> hideEditDialog()
            is BookshelfIntent.UpdateBookshelf -> updateBookshelf(intent.id, intent.name, intent.description)
            is BookshelfIntent.ShowDeleteDialog -> showDeleteDialog(intent.bookshelf)
            is BookshelfIntent.HideDeleteDialog -> hideDeleteDialog()
            is BookshelfIntent.DeleteBookshelf -> deleteBookshelf(intent.id)
            is BookshelfIntent.RemoveBookFromBookshelf -> removeBookFromBookshelf(intent.bookshelfId, intent.bookId)
            is BookshelfIntent.OpenBookDetail -> openBookDetail(intent.bookId)
            is BookshelfIntent.StartReading -> startReading(intent.bookId)
            // 添加到书架对话框
            is BookshelfIntent.ShowAddToBookshelvesDialog -> showAddToBookshelvesDialog(intent.book)
            is BookshelfIntent.HideAddToBookshelvesDialog -> hideAddToBookshelvesDialog()
            is BookshelfIntent.ToggleAddToBookshelfSelection -> toggleAddToBookshelfSelection(intent.bookshelfId)
            is BookshelfIntent.ConfirmAddToBookshelves -> confirmAddToBookshelves(intent.bookId)
            // 移动到书架对话框
            is BookshelfIntent.ShowMoveToBookshelfDialog -> showMoveToBookshelfDialog(intent.book)
            is BookshelfIntent.HideMoveToBookshelfDialog -> hideMoveToBookshelfDialog()
            is BookshelfIntent.SelectMoveToBookshelf -> selectMoveToBookshelf(intent.bookshelfId)
            is BookshelfIntent.ConfirmMoveToBookshelf -> confirmMoveToBookshelf(intent.bookId)
            // 从所有书架移除对话框
            is BookshelfIntent.ShowRemoveFromAllDialog -> showRemoveFromAllDialog(intent.book)
            is BookshelfIntent.HideRemoveFromAllDialog -> hideRemoveFromAllDialog()
            is BookshelfIntent.ConfirmRemoveFromAll -> confirmRemoveFromAll(intent.bookId)
        }
    }
    
    // ==================== 初始化和刷新 ====================
    
    private fun initialize() {
        if (_state.value.isLoadingBookshelves) return
        
        scope.launch {
            _state.update { it.copy(isLoadingBookshelves = true, error = null) }
            
            bookshelfRepository.getBookshelves().fold(
                onSuccess = { bookshelves ->
                    // 尝试恢复上次选中的书架，如果不存在则选择第一个
                    val lastSelectedId = preferenceRepository.lastSelectedBookshelfId
                    val selectedBookshelf = if (lastSelectedId != -1) {
                        bookshelves.find { it.id == lastSelectedId } ?: bookshelves.firstOrNull()
                    } else {
                        bookshelves.firstOrNull()
                    }
                    
                    _state.update { 
                        it.copy(
                            bookshelves = bookshelves,
                            selectedBookshelfId = selectedBookshelf?.id,
                            isLoadingBookshelves = false,
                            error = null
                        )
                    }
                    
                    // 自动加载选中书架的书籍
                    selectedBookshelf?.let { loadBooks(it.id) }
                    
                    // 后台同步网络数据
                    syncBookshelves()
                },
                onFailure = { e ->
                    _state.update { 
                        it.copy(
                            isLoadingBookshelves = false,
                            error = e.message
                        )
                    }
                    throw e
                }
            )
        }
    }
    
    private fun refresh(showRefreshingUI: Boolean = true) {
        scope.launch {
            if (showRefreshingUI) {
                _state.update { it.copy(isRefreshing = true, error = null) }
            }
            
            bookshelfRepository.getBookshelves(forceRefresh = true).fold(
                onSuccess = { bookshelves ->
                    val currentSelected = _state.value.selectedBookshelfId
                    val newSelected = if (currentSelected != null && bookshelves.any { it.id == currentSelected }) {
                        currentSelected
                    } else {
                        bookshelves.firstOrNull()?.id
                    }
                    
                    _state.update { 
                        it.copy(
                            bookshelves = bookshelves,
                            selectedBookshelfId = newSelected,
                            isRefreshing = false,
                            error = null
                        )
                    }
                    
                    // 刷新当前选中书架的书籍
                    newSelected?.let { loadBooks(it, forceRefresh = true) }
                },
                onFailure = { e ->
                    _state.update { 
                        it.copy(
                            isRefreshing = false,
                            error = e.message
                        )
                    }
                    throw e
                }
            )
        }
    }
    
    private fun syncBookshelves() {
        scope.launch {
            bookshelfRepository.syncBookshelves().fold(
                onSuccess = { bookshelves ->
                    _state.update { it.copy(bookshelves = bookshelves) }
                },
                onFailure = {
                    // 后台同步失败不影响用户体验，静默处理
                }
            )
        }
    }
    
    // ==================== 书架选择和书籍加载 ====================
    
    private fun selectBookshelf(bookshelfId: Int) {
        if (_state.value.selectedBookshelfId == bookshelfId) return
        
        _state.update { it.copy(selectedBookshelfId = bookshelfId) }
        
        // 保存选中的书架 ID 到偏好设置
        preferenceRepository.lastSelectedBookshelfId = bookshelfId
        
        // 如果该书架的书籍尚未加载，则加载
        if (_state.value.booksByBookshelf[bookshelfId] == null) {
            loadBooks(bookshelfId)
        }
    }
    
    private fun loadBooks(bookshelfId: Int, forceRefresh: Boolean = false) {
        if (_state.value.loadingBooksForBookshelf == bookshelfId) return
        
        scope.launch {
            _state.update { it.copy(loadingBooksForBookshelf = bookshelfId) }
            
            bookshelfRepository.getBooksInBookshelf(bookshelfId, offset = 0, forceRefresh = forceRefresh).fold(
                onSuccess = { response ->
                    _state.update { state ->
                        state.copy(
                            booksByBookshelf = state.booksByBookshelf + (bookshelfId to response.books),
                            totalByBookshelf = state.totalByBookshelf + (bookshelfId to response.total),
                            hasMoreByBookshelf = state.hasMoreByBookshelf + (bookshelfId to response.hasMore),
                            loadingBooksForBookshelf = null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(loadingBooksForBookshelf = null, error = e.message) }
                    throw e
                }
            )
        }
    }
    
    private fun loadMoreBooks(bookshelfId: Int) {
        val currentState = _state.value
        if (currentState.loadingMoreForBookshelf == bookshelfId) return
        if (currentState.hasMoreByBookshelf[bookshelfId] != true) return
        
        val currentBooks = currentState.booksByBookshelf[bookshelfId] ?: emptyList()
        val offset = currentBooks.size.toLong()
        
        scope.launch {
            _state.update { it.copy(loadingMoreForBookshelf = bookshelfId) }
            
            bookshelfRepository.loadMoreBooks(bookshelfId, offset).fold(
                onSuccess = { response ->
                    _state.update { state ->
                        val existingBooks = state.booksByBookshelf[bookshelfId] ?: emptyList()
                        state.copy(
                            booksByBookshelf = state.booksByBookshelf + (bookshelfId to existingBooks + response.books),
                            totalByBookshelf = state.totalByBookshelf + (bookshelfId to response.total),
                            hasMoreByBookshelf = state.hasMoreByBookshelf + (bookshelfId to response.hasMore),
                            loadingMoreForBookshelf = null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(loadingMoreForBookshelf = null, error = e.message) }
                    throw e
                }
            )
        }
    }
    
    // ==================== 视图控制 ====================
    
    private fun toggleCollapse(bookshelfId: Int) {
        _state.update { state ->
            val newCollapsed = if (bookshelfId in state.collapsedBookshelves) {
                state.collapsedBookshelves - bookshelfId
            } else {
                state.collapsedBookshelves + bookshelfId
            }
            state.copy(collapsedBookshelves = newCollapsed)
        }
    }
    
    private fun toggleViewMode() {
        preferenceRepository.toggleViewMode()
        _state.update { it.copy(viewMode = preferenceRepository.viewMode) }
    }
    
    // ==================== 书架管理 ====================
    
    private fun showCreateDialog() {
        _state.update { it.copy(showCreateDialog = true) }
    }
    
    private fun hideCreateDialog() {
        _state.update { it.copy(showCreateDialog = false) }
    }
    
    private fun createBookshelf(name: String, description: String?) {
        scope.launch {
            _state.update { it.copy(isDialogLoading = true) }
            
            bookshelfRepository.createBookshelf(name, description).fold(
                onSuccess = { newBookshelf ->
                    _state.update { state ->
                        state.copy(
                            bookshelves = state.bookshelves + newBookshelf,
                            showCreateDialog = false,
                            isDialogLoading = false
                        )
                    }
                    _effect.emit(BookshelfEffect.BookshelfCreated)
                },
                onFailure = { e ->
                    _state.update { it.copy(isDialogLoading = false) }
                    throw e
                }
            )
        }
    }
    
    private fun showEditDialog(bookshelf: Bookshelf) {
        _state.update { it.copy(showEditDialog = true, editingBookshelf = bookshelf) }
    }
    
    private fun hideEditDialog() {
        _state.update { it.copy(showEditDialog = false, editingBookshelf = null) }
    }
    
    private fun updateBookshelf(id: Int, name: String?, description: String?) {
        scope.launch {
            _state.update { it.copy(isDialogLoading = true) }
            
            bookshelfRepository.updateBookshelf(id, name, description).fold(
                onSuccess = { updatedBookshelf ->
                    _state.update { state ->
                        state.copy(
                            bookshelves = state.bookshelves.map { 
                                if (it.id == id) updatedBookshelf else it 
                            },
                            showEditDialog = false,
                            editingBookshelf = null,
                            isDialogLoading = false
                        )
                    }
                    _effect.emit(BookshelfEffect.BookshelfUpdated)
                },
                onFailure = { e ->
                    _state.update { it.copy(isDialogLoading = false) }
                    throw e
                }
            )
        }
    }
    
    private fun showDeleteDialog(bookshelf: Bookshelf) {
        _state.update { it.copy(showDeleteDialog = true, editingBookshelf = bookshelf) }
    }
    
    private fun hideDeleteDialog() {
        _state.update { it.copy(showDeleteDialog = false, editingBookshelf = null) }
    }
    
    private fun deleteBookshelf(id: Int) {
        scope.launch {
            _state.update { it.copy(isDialogLoading = true) }
            
            bookshelfRepository.deleteBookshelf(id).fold(
                onSuccess = {
                    val newBookshelves = _state.value.bookshelves.filter { it.id != id }
                    val newSelected = if (_state.value.selectedBookshelfId == id) {
                        newBookshelves.firstOrNull()?.id
                    } else {
                        _state.value.selectedBookshelfId
                    }
                    
                    _state.update { state ->
                        state.copy(
                            bookshelves = newBookshelves,
                            selectedBookshelfId = newSelected,
                            booksByBookshelf = state.booksByBookshelf - id,
                            totalByBookshelf = state.totalByBookshelf - id,
                            hasMoreByBookshelf = state.hasMoreByBookshelf - id,
                            showDeleteDialog = false,
                            editingBookshelf = null,
                            isDialogLoading = false
                        )
                    }
                    _effect.emit(BookshelfEffect.BookshelfDeleted)
                },
                onFailure = { e ->
                    _state.update { it.copy(isDialogLoading = false) }
                    throw e
                }
            )
        }
    }
    
    private fun removeBookFromBookshelf(bookshelfId: Int, bookId: Int) {
        scope.launch {
            bookshelfRepository.removeBookFromBookshelf(bookshelfId, bookId).fold(
                onSuccess = {
                    // 从本地状态移除书籍
                    _state.update { state ->
                        val currentBooks = state.booksByBookshelf[bookshelfId] ?: emptyList()
                        val newBooks = currentBooks.filter { it.book.id != bookId }
                        val currentTotal = state.totalByBookshelf[bookshelfId] ?: 0
                        
                        // 更新书架的 bookCount
                        val updatedBookshelves = state.bookshelves.map { bookshelf ->
                            if (bookshelf.id == bookshelfId) {
                                bookshelf.copy(bookCount = (bookshelf.bookCount - 1).coerceAtLeast(0))
                            } else {
                                bookshelf
                            }
                        }
                        
                        state.copy(
                            bookshelves = updatedBookshelves,
                            booksByBookshelf = state.booksByBookshelf + (bookshelfId to newBooks),
                            totalByBookshelf = state.totalByBookshelf + (bookshelfId to (currentTotal - 1).coerceAtLeast(0))
                        )
                    }
                    _effect.emit(BookshelfEffect.BookRemoved)
                },
                onFailure = { e ->
                    throw e
                }
            )
        }
    }
    
    // ==================== 导航 ====================
    
    private fun openBookDetail(bookId: Int) {
        scope.launch {
            _effect.emit(BookshelfEffect.NavigateToBookDetail(bookId))
        }
    }
    
    private fun startReading(bookId: Int) {
        scope.launch {
            _effect.emit(BookshelfEffect.NavigateToReader(bookId))
        }
    }
    
    // ==================== 添加到书架对话框 ====================
    
    private fun showAddToBookshelvesDialog(book: BookWithProgress) {
        scope.launch {
            _state.update { 
                it.copy(
                    showAddToBookshelvesDialog = true,
                    addToBookshelvesBook = book,
                    addToBookshelvesAvailable = emptyList(),
                    addToBookshelvesSelected = emptySet(),
                    isLoadingAddToBookshelves = true
                )
            }
            
            // 获取书籍当前所属的书架
            bookshelfRepository.getBookshelvesForBook(book.book.id).fold(
                onSuccess = { bookBookshelves ->
                    val bookBookshelfIds = bookBookshelves.map { it.id }.toSet()
                    // 过滤出书籍未加入的书架（排除系统默认书架）
                    val availableBookshelves = _state.value.bookshelves.filter { 
                        it.id !in bookBookshelfIds && !it.isSystemDefault
                    }
                    _state.update { 
                        it.copy(
                            addToBookshelvesAvailable = availableBookshelves,
                            isLoadingAddToBookshelves = false
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoadingAddToBookshelves = false) }
                    throw e
                }
            )
        }
    }
    
    private fun hideAddToBookshelvesDialog() {
        _state.update { 
            it.copy(
                showAddToBookshelvesDialog = false,
                addToBookshelvesBook = null,
                addToBookshelvesAvailable = emptyList(),
                addToBookshelvesSelected = emptySet(),
                isLoadingAddToBookshelves = false,
                isAddingToBookshelves = false
            )
        }
    }
    
    private fun toggleAddToBookshelfSelection(bookshelfId: Int) {
        _state.update { state ->
            val newSelected = if (bookshelfId in state.addToBookshelvesSelected) {
                state.addToBookshelvesSelected - bookshelfId
            } else {
                state.addToBookshelvesSelected + bookshelfId
            }
            state.copy(addToBookshelvesSelected = newSelected)
        }
    }
    
    private fun confirmAddToBookshelves(bookId: Int) {
        val selectedIds = _state.value.addToBookshelvesSelected.toList()
        
        if (selectedIds.isEmpty()) {
            hideAddToBookshelvesDialog()
            return
        }
        
        scope.launch {
            _state.update { it.copy(isAddingToBookshelves = true) }
            
            bookshelfRepository.addBookToBookshelves(bookId, selectedIds).fold(
                onSuccess = {
                    // 刷新当前书架的书籍列表
                    _state.value.selectedBookshelfId?.let { currentBookshelfId ->
                        loadBooks(currentBookshelfId, forceRefresh = true)
                    }
                    
                    // 同步刷新书架列表（更新 bookCount）
                    syncBookshelves()
                    
                    hideAddToBookshelvesDialog()
                    _effect.emit(BookshelfEffect.BookAddedToBookshelves)
                },
                onFailure = { e ->
                    _state.update { it.copy(isAddingToBookshelves = false) }
                    throw e
                }
            )
        }
    }
    
    // ==================== 移动到书架对话框 ====================
    
    private fun showMoveToBookshelfDialog(book: BookWithProgress) {
        val currentBookshelfId = _state.value.selectedBookshelfId
        
        scope.launch {
            _state.update { 
                it.copy(
                    showMoveToBookshelfDialog = true,
                    moveToBookshelfBook = book,
                    moveToBookshelfAvailable = emptyList(),
                    moveToBookshelfSelected = null,
                    isLoadingMoveToBookshelf = true
                )
            }
            
            // 过滤出可移动到的书架（排除当前书架和系统默认书架）
            val availableBookshelves = _state.value.bookshelves.filter { 
                it.id != currentBookshelfId && !it.isSystemDefault
            }
            
            _state.update { 
                it.copy(
                    moveToBookshelfAvailable = availableBookshelves,
                    isLoadingMoveToBookshelf = false
                )
            }
        }
    }
    
    private fun hideMoveToBookshelfDialog() {
        _state.update { 
            it.copy(
                showMoveToBookshelfDialog = false,
                moveToBookshelfBook = null,
                moveToBookshelfAvailable = emptyList(),
                moveToBookshelfSelected = null,
                isLoadingMoveToBookshelf = false,
                isMovingToBookshelf = false
            )
        }
    }
    
    private fun selectMoveToBookshelf(bookshelfId: Int) {
        _state.update { state ->
            // 单选：如果已选中则取消，否则选中
            val newSelected = if (state.moveToBookshelfSelected == bookshelfId) null else bookshelfId
            state.copy(moveToBookshelfSelected = newSelected)
        }
    }
    
    private fun confirmMoveToBookshelf(bookId: Int) {
        val currentBookshelfId = _state.value.selectedBookshelfId ?: return
        val targetBookshelfId = _state.value.moveToBookshelfSelected ?: run {
            hideMoveToBookshelfDialog()
            return
        }
        
        scope.launch {
            _state.update { it.copy(isMovingToBookshelf = true) }
            
            // 先从当前书架移除
            bookshelfRepository.removeBookFromBookshelf(currentBookshelfId, bookId).fold(
                onSuccess = {
                    // 再添加到目标书架
                    bookshelfRepository.addBookToBookshelf(targetBookshelfId, bookId).fold(
                        onSuccess = {
                            // 从当前书架的本地状态移除书籍
                            _state.update { state ->
                                val currentBooks = state.booksByBookshelf[currentBookshelfId] ?: emptyList()
                                val newBooks = currentBooks.filter { it.book.id != bookId }
                                val currentTotal = state.totalByBookshelf[currentBookshelfId] ?: 0
                                
                                // 更新书架的 bookCount
                                val updatedBookshelves = state.bookshelves.map { bookshelf ->
                                    when (bookshelf.id) {
                                        currentBookshelfId -> bookshelf.copy(bookCount = (bookshelf.bookCount - 1).coerceAtLeast(0))
                                        targetBookshelfId -> bookshelf.copy(bookCount = bookshelf.bookCount + 1)
                                        else -> bookshelf
                                    }
                                }
                                
                                state.copy(
                                    bookshelves = updatedBookshelves,
                                    booksByBookshelf = state.booksByBookshelf + (currentBookshelfId to newBooks),
                                    totalByBookshelf = state.totalByBookshelf + (currentBookshelfId to (currentTotal - 1).coerceAtLeast(0))
                                )
                            }
                            
                            hideMoveToBookshelfDialog()
                            _effect.emit(BookshelfEffect.BookMovedToBookshelf)
                        },
                        onFailure = { e ->
                            _state.update { it.copy(isMovingToBookshelf = false) }
                            throw e
                        }
                    )
                },
                onFailure = { e ->
                    _state.update { it.copy(isMovingToBookshelf = false) }
                    throw e
                }
            )
        }
    }
    
    // ==================== 从所有书架移除对话框 ====================
    
    private fun showRemoveFromAllDialog(book: BookWithProgress) {
        _state.update { 
            it.copy(
                showRemoveFromAllDialog = true,
                removeFromAllBook = book
            )
        }
    }
    
    private fun hideRemoveFromAllDialog() {
        _state.update { 
            it.copy(
                showRemoveFromAllDialog = false,
                removeFromAllBook = null,
                isRemovingFromAll = false
            )
        }
    }
    
    private fun confirmRemoveFromAll(bookId: Int) {
        scope.launch {
            _state.update { it.copy(isRemovingFromAll = true) }
            
            // 获取书籍当前所属的所有书架
            bookshelfRepository.getBookshelvesForBook(bookId).fold(
                onSuccess = { bookBookshelves ->
                    val bookshelfIds = bookBookshelves.map { it.id }
                    
                    if (bookshelfIds.isEmpty()) {
                        hideRemoveFromAllDialog()
                        return@launch
                    }
                    
                    // 从所有书架移除
                    bookshelfRepository.removeBookFromBookshelves(bookId, bookshelfIds).fold(
                        onSuccess = {
                            // 从本地状态移除书籍
                            _state.update { state ->
                                val updatedBooksByBookshelf = state.booksByBookshelf.mapValues { (bookshelfId, books) ->
                                    if (bookshelfId in bookshelfIds) {
                                        books.filter { it.book.id != bookId }
                                    } else {
                                        books
                                    }
                                }
                                val updatedTotalByBookshelf = state.totalByBookshelf.mapValues { (bookshelfId, total) ->
                                    if (bookshelfId in bookshelfIds) {
                                        (total - 1).coerceAtLeast(0)
                                    } else {
                                        total
                                    }
                                }
                                // 更新书架的 bookCount
                                val updatedBookshelves = state.bookshelves.map { bookshelf ->
                                    if (bookshelf.id in bookshelfIds) {
                                        bookshelf.copy(bookCount = (bookshelf.bookCount - 1).coerceAtLeast(0))
                                    } else {
                                        bookshelf
                                    }
                                }
                                
                                state.copy(
                                    bookshelves = updatedBookshelves,
                                    booksByBookshelf = updatedBooksByBookshelf,
                                    totalByBookshelf = updatedTotalByBookshelf
                                )
                            }
                            
                            hideRemoveFromAllDialog()
                            _effect.emit(BookshelfEffect.BookRemovedFromAll)
                        },
                        onFailure = { e ->
                            _state.update { it.copy(isRemovingFromAll = false) }
                            throw e
                        }
                    )
                },
                onFailure = { e ->
                    _state.update { it.copy(isRemovingFromAll = false) }
                    throw e
                }
            )
        }
    }
}
