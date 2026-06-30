package com.bookd.app.data.vm

import com.bookd.app.basic.lifecycle.BaseViewModel
import com.bookd.app.data.model.Book
import com.bookd.app.data.repository.BookRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchBookState(
    val query: String = "",
    val submittedQuery: String = "",
    val books: List<Book> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val error: String? = null
) {
    val isInitial: Boolean
        get() = !hasSearched && books.isEmpty() && !isLoading

    val isEmptyResult: Boolean
        get() = hasSearched && books.isEmpty() && !isLoading && error == null
}

sealed class SearchBookIntent {
    data class QueryChanged(val query: String) : SearchBookIntent()
    data object SubmitSearch : SearchBookIntent()
    data object LoadMore : SearchBookIntent()
    data class SelectBook(val bookId: Int) : SearchBookIntent()
}

sealed class SearchBookEffect {
    data class NavigateToBookDetail(val bookId: Int) : SearchBookEffect()
}

class SearchBookViewModel(
    private val bookRepository: BookRepository
) : BaseViewModel() {

    private val _state = MutableStateFlow(SearchBookState())
    val state: StateFlow<SearchBookState> = _state.asStateFlow()

    private val _effect = Channel<SearchBookEffect>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    fun onIntent(intent: SearchBookIntent) {
        when (intent) {
            is SearchBookIntent.QueryChanged -> updateQuery(intent.query)
            is SearchBookIntent.SubmitSearch -> searchFirstPage()
            is SearchBookIntent.LoadMore -> loadMore()
            is SearchBookIntent.SelectBook -> selectBook(intent.bookId)
        }
    }

    private fun updateQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    private fun searchFirstPage() {
        val trimmedQuery = _state.value.query.trim()
        if (_state.value.isLoading || _state.value.isLoadingMore) return

        if (trimmedQuery.isBlank()) {
            _state.update {
                SearchBookState(query = it.query)
            }
            return
        }

        scope.launch {
            _state.update {
                it.copy(
                    submittedQuery = trimmedQuery,
                    books = emptyList(),
                    total = 0,
                    hasMore = false,
                    isLoading = true,
                    isLoadingMore = false,
                    hasSearched = true,
                    error = null
                )
            }

            bookRepository.searchBooks(trimmedQuery, offset = 0).fold(
                onSuccess = { response ->
                    _state.update {
                        it.copy(
                            books = response.books,
                            total = response.total,
                            hasMore = response.hasMore,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            books = emptyList(),
                            total = 0,
                            hasMore = false,
                            isLoading = false,
                            error = e.message ?: ""
                        )
                    }
                    throw e
                }
            )
        }
    }

    private fun loadMore() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.hasMore) return
        val submittedQuery = currentState.submittedQuery
        if (submittedQuery.isBlank()) return

        scope.launch {
            _state.update { it.copy(isLoadingMore = true, error = null) }
            val offset = currentState.books.size.toLong()

            bookRepository.searchBooks(submittedQuery, offset = offset).fold(
                onSuccess = { response ->
                    _state.update {
                        it.copy(
                            books = it.books + response.books,
                            total = response.total,
                            hasMore = response.hasMore,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isLoadingMore = false, error = e.message ?: "")
                    }
                    throw e
                }
            )
        }
    }

    private fun selectBook(bookId: Int) {
        _effect.trySend(SearchBookEffect.NavigateToBookDetail(bookId))
    }
}
