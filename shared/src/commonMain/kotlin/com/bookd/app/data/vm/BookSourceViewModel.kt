package com.bookd.app.data.vm

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.data.model.Book
import com.bookd.app.data.model.BookSource
import com.bookd.app.data.repository.BookRepository
import com.bookd.app.data.repository.BookSourceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * 书源页面状态
 */
data class BookSourceState(
    // 书源列表
    val sources: List<BookSource> = emptyList(),
    val sourcesLoading: Boolean = false,
    val sourcesError: String? = null,
    
    // 当前选中的书源索引
    val selectedSourceIndex: Int = 0,
    
    // 书籍列表（按 sourceId 分组）
    val booksBySource: Map<Int, List<Book>> = emptyMap(),
    val booksLoading: Map<Int, Boolean> = emptyMap(),
    val booksLoadingMore: Map<Int, Boolean> = emptyMap(),
    val booksRefreshing: Map<Int, Boolean> = emptyMap(),
    val booksError: Map<Int, String?> = emptyMap(),
    val booksHasMore: Map<Int, Boolean> = emptyMap(),
    val booksTotal: Map<Int, Int> = emptyMap()
) {
    /**
     * 当前选中的书源
     */
    val currentSource: BookSource?
        get() = sources.getOrNull(selectedSourceIndex)
    
    /**
     * 当前书源的书籍列表
     */
    val currentBooks: List<Book>
        get() = currentSource?.let { booksBySource[it.id] } ?: emptyList()
    
    /**
     * 当前书源是否正在加载
     */
    val isCurrentLoading: Boolean
        get() = currentSource?.let { booksLoading[it.id] } ?: false
    
    /**
     * 当前书源是否正在加载更多
     */
    val isCurrentLoadingMore: Boolean
        get() = currentSource?.let { booksLoadingMore[it.id] } ?: false
    
    /**
     * 当前书源是否正在刷新
     */
    val isCurrentRefreshing: Boolean
        get() = currentSource?.let { booksRefreshing[it.id] } ?: false
    
    /**
     * 当前书源的错误信息
     */
    val currentError: String?
        get() = currentSource?.let { booksError[it.id] }
    
    /**
     * 当前书源是否还有更多数据
     */
    val currentHasMore: Boolean
        get() = currentSource?.let { booksHasMore[it.id] } ?: true
}

/**
 * 用户意图
 */
sealed class BookSourceIntent {
    /** 加载书源列表 */
    data object LoadSources : BookSourceIntent()
    
    /** 选择书源 */
    data class SelectSource(val index: Int) : BookSourceIntent()
    
    /** 加载书籍列表 */
    data class LoadBooks(val sourceId: Int, val forceRefresh: Boolean = false) : BookSourceIntent()
    
    /** 加载更多书籍 */
    data class LoadMoreBooks(val sourceId: Int) : BookSourceIntent()
    
    /** 刷新当前书源的书籍 */
    data object RefreshCurrentBooks : BookSourceIntent()
    
    /** 刷新所有数据（书源 + 所有书源的首屏书籍） */
    data object RefreshAll : BookSourceIntent()
}

/**
 * 一次性效果
 */
sealed class BookSourceEffect {
    /** 滚动到顶部 */
    data class ScrollToTop(val sourceId: Int) : BookSourceEffect()
}

class BookSourceViewModel(
    private val bookSourceRepository: BookSourceRepository,
    private val bookRepository: BookRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(BookSourceState())
    val state: StateFlow<BookSourceState> = _state.asStateFlow()
    
    private val _effect = MutableSharedFlow<BookSourceEffect>()
    val effect: SharedFlow<BookSourceEffect> = _effect.asSharedFlow()

    // 每个 page 独立的滚动状态（懒加载）
    private val _scrollStates = mutableStateMapOf<Int, LazyListState>()

    // Tab 行折叠偏移量
    var tabRowOffset by mutableFloatStateOf(0f)
    
    /**
     * 获取或创建指定 page 的滚动状态
     * 按需创建，避免提前初始化所有页面的状态
     */
    fun getScrollState(page: Int): LazyListState {
        return _scrollStates.getOrPut(page) { LazyListState() }
    }
    
    /**
     * 处理用户意图
     */
    fun onIntent(intent: BookSourceIntent) {
        when (intent) {
            is BookSourceIntent.LoadSources -> loadSources()
            is BookSourceIntent.SelectSource -> selectSource(intent.index)
            is BookSourceIntent.LoadBooks -> loadBooks(intent.sourceId, intent.forceRefresh)
            is BookSourceIntent.LoadMoreBooks -> loadMoreBooks(intent.sourceId)
            is BookSourceIntent.RefreshCurrentBooks -> refreshCurrentBooks()
            is BookSourceIntent.RefreshAll -> refreshAll()
        }
    }
    
    private fun loadSources(forceRefresh: Boolean = false) {
        scope.launch {
            val sources = loadSourcesInternal(forceRefresh)

            // 自动加载第一个书源的书籍
            sources.firstOrNull()?.let { source ->
                if (_state.value.booksBySource[source.id].isNullOrEmpty()) {
                    loadBooks(source.id)
                }
            }
        }
    }

    private suspend fun loadSourcesInternal(forceRefresh: Boolean = false): List<BookSource> {
        _state.update { it.copy(sourcesLoading = true, sourcesError = null) }

        return bookSourceRepository.getSources(forceRefresh).fold(
            onSuccess = { sources ->
                _state.update {
                    it.copy(
                        sources = sources,
                        sourcesLoading = false,
                        // 如果当前索引超出范围，重置为 0
                        selectedSourceIndex = if (it.selectedSourceIndex >= sources.size) 0 else it.selectedSourceIndex
                    )
                }
                sources
            },
            onFailure = { e ->
                _state.update {
                    it.copy(
                        sourcesLoading = false,
                        sourcesError = e.message
                    )
                }
                throw e
            }
        )
    }
    
    private fun selectSource(index: Int) {
        val sources = _state.value.sources
        if (index < 0 || index >= sources.size) return
        
        _state.update { it.copy(selectedSourceIndex = index) }
        
        // 如果该书源没有加载过书籍，自动加载
        val source = sources[index]
        if (_state.value.booksBySource[source.id].isNullOrEmpty() && 
            _state.value.booksLoading[source.id] != true) {
            loadBooks(source.id)
        }
    }
    
    private fun loadBooks(sourceId: Int, forceRefresh: Boolean = false) {
        scope.launch {
            loadBooksInternal(sourceId, forceRefresh, scrollToTop = forceRefresh)
        }
    }

    private suspend fun loadBooksInternal(
        sourceId: Int,
        forceRefresh: Boolean = false,
        scrollToTop: Boolean = forceRefresh
    ) {
        // 如果正在加载，跳过
        if (_state.value.booksLoading[sourceId] == true) return

        _state.update { state ->
            state.copy(
                booksLoading = state.booksLoading + (sourceId to true),
                booksRefreshing = if (forceRefresh) state.booksRefreshing + (sourceId to true) else state.booksRefreshing,
                booksError = state.booksError + (sourceId to null)
            )
        }

        bookRepository.getBooks(sourceId, offset = 0, forceRefresh = forceRefresh).fold(
            onSuccess = { response ->
                _state.update { state ->
                    state.copy(
                        booksBySource = state.booksBySource + (sourceId to response.books),
                        booksLoading = state.booksLoading + (sourceId to false),
                        booksRefreshing = state.booksRefreshing + (sourceId to false),
                        booksHasMore = state.booksHasMore + (sourceId to response.hasMore),
                        booksTotal = state.booksTotal + (sourceId to response.total)
                    )
                }

                if (scrollToTop) {
                    _effect.emit(BookSourceEffect.ScrollToTop(sourceId))
                }
            },
            onFailure = { e ->
                _state.update { state ->
                    state.copy(
                        booksLoading = state.booksLoading + (sourceId to false),
                        booksRefreshing = state.booksRefreshing + (sourceId to false),
                        booksError = state.booksError + (sourceId to e.message)
                    )
                }
                throw e
            }
        )
    }
    
    private fun loadMoreBooks(sourceId: Int) {
        // 如果正在加载或加载更多，跳过
        if (_state.value.booksLoading[sourceId] == true || 
            _state.value.booksLoadingMore[sourceId] == true) return
        
        // 如果没有更多数据，跳过
        if (_state.value.booksHasMore[sourceId] == false) return
        
        val currentBooks = _state.value.booksBySource[sourceId] ?: emptyList()
        val offset = currentBooks.size.toLong()
        
        scope.launch {
            _state.update { state ->
                state.copy(booksLoadingMore = state.booksLoadingMore + (sourceId to true))
            }
            
            bookRepository.loadMore(sourceId, offset).fold(
                onSuccess = { response ->
                    _state.update { state ->
                        val existingBooks = state.booksBySource[sourceId] ?: emptyList()
                        state.copy(
                            booksBySource = state.booksBySource + (sourceId to (existingBooks + response.books)),
                            booksLoadingMore = state.booksLoadingMore + (sourceId to false),
                            booksHasMore = state.booksHasMore + (sourceId to response.hasMore),
                            booksTotal = state.booksTotal + (sourceId to response.total)
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { state ->
                        state.copy(booksLoadingMore = state.booksLoadingMore + (sourceId to false))
                    }
                    throw e
                }
            )
        }
    }
    
    private fun refreshCurrentBooks() {
        _state.value.currentSource?.let { source ->
            loadBooks(source.id, forceRefresh = true)
        }
    }
    
    private fun refreshAll() {
        if (_state.value.sourcesLoading || hasActiveBookOperation()) return

        scope.launch {
            val currentSourceId = _state.value.currentSource?.id
            val sources = loadSourcesInternal(forceRefresh = true)
            val failures = mutableListOf<Throwable>()

            sources.forEach { source ->
                try {
                    loadBooksInternal(
                        sourceId = source.id,
                        forceRefresh = true,
                        scrollToTop = source.id == currentSourceId
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Throwable) {
                    failures += e
                }
            }

            failures.firstOrNull()?.let { throw it }
        }
    }

    private fun hasActiveBookOperation(): Boolean {
        val state = _state.value
        return state.booksLoading.values.any { it } ||
            state.booksLoadingMore.values.any { it } ||
            state.booksRefreshing.values.any { it }
    }
}
