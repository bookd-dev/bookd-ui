package com.bookd.app.screen.bookshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.composeapp.generated.resources.Res
import app.composeapp.generated.resources.back
import app.composeapp.generated.resources.chapters_count
import app.composeapp.generated.resources.close
import app.composeapp.generated.resources.loading
import app.composeapp.generated.resources.no_more_data
import app.composeapp.generated.resources.search
import app.composeapp.generated.resources.search_book_empty
import app.composeapp.generated.resources.search_book_hint
import app.composeapp.generated.resources.search_book_initial
import app.composeapp.generated.resources.search_book_load_failed
import coil3.compose.AsyncImage
import com.bookd.app.data.model.Book
import com.bookd.app.data.vm.SearchBookEffect
import com.bookd.app.data.vm.SearchBookIntent
import com.bookd.app.data.vm.SearchBookState
import com.bookd.app.data.vm.SearchBookViewModel
import com.bookd.app.screen.RouteBookDetail
import com.bookd.app.screen.rememberScreenContext
import com.bookd.app.ui.AppPreviewContent
import com.bookd.app.ui.AppVerticalZHPreview
import com.bookd.app.ui.theme.FormatEpub
import com.bookd.app.ui.theme.FormatMobi
import com.bookd.app.ui.theme.FormatPdf
import com.bookd.app.ui.theme.FormatTxt
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource

@Composable
fun SearchBookScreen() {
    val screenContext = rememberScreenContext<SearchBookViewModel>()
    val viewModel = screenContext.viewModel
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is SearchBookEffect.NavigateToBookDetail -> {
                    screenContext.navigator.navigateTo(RouteBookDetail(effect.bookId))
                }
            }
        }
    }

    SearchBookContent(
        state = state,
        onBackClick = { screenContext.navigator.navigateBack() },
        onQueryChanged = { viewModel.onIntent(SearchBookIntent.QueryChanged(it)) },
        onSubmitSearch = { viewModel.onIntent(SearchBookIntent.SubmitSearch) },
        onLoadMore = { viewModel.onIntent(SearchBookIntent.LoadMore) },
        onBookClick = { book -> viewModel.onIntent(SearchBookIntent.SelectBook(book.id)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBookContent(
    state: SearchBookState,
    onBackClick: () -> Unit = {},
    onQueryChanged: (String) -> Unit = {},
    onSubmitSearch: () -> Unit = {},
    onLoadMore: () -> Unit = {},
    onBookClick: (Book) -> Unit = {}
) {
    val focusRequester = FocusRequester()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.search)) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchInput(
                query = state.query,
                isLoading = state.isLoading,
                focusRequester = focusRequester,
                onQueryChanged = onQueryChanged,
                onSubmitSearch = onSubmitSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            SearchResultContent(
                state = state,
                onLoadMore = onLoadMore,
                onBookClick = onBookClick
            )
        }
    }
}

@Composable
private fun SearchInput(
    query: String,
    isLoading: Boolean,
    focusRequester: FocusRequester,
    onQueryChanged: (String) -> Unit,
    onSubmitSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        singleLine = true,
        enabled = !isLoading,
        label = { Text(stringResource(Res.string.search_book_hint)) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(Res.string.close)
                    )
                }
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = ImeAction.Search
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onSearch = { onSubmitSearch() }
        )
    )
}

@Composable
private fun SearchResultContent(
    state: SearchBookState,
    onLoadMore: () -> Unit,
    onBookClick: (Book) -> Unit
) {
    when {
        state.isLoading -> CenterState {
            CircularProgressIndicator()
        }

        state.isInitial -> CenterState {
            Text(
                text = stringResource(Res.string.search_book_initial),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.isEmptyResult -> CenterState {
            Text(
                text = stringResource(Res.string.search_book_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        state.error != null && state.books.isEmpty() -> CenterState {
            Text(
                text = state.error.ifBlank { stringResource(Res.string.search_book_load_failed) },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }

        else -> SearchResultList(
            state = state,
            onLoadMore = onLoadMore,
            onBookClick = onBookClick
        )
    }
}

@Composable
private fun CenterState(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SearchResultList(
    state: SearchBookState,
    onLoadMore: () -> Unit,
    onBookClick: (Book) -> Unit
) {
    val listState = rememberLazyListState()
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    LaunchedEffect(listState, state.books.size, state.hasMore) {
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
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        items(
            items = state.books,
            key = { it.id },
            contentType = { "search_book_result" }
        ) { book ->
            SearchBookResultItem(
                book = book,
                onClick = { onBookClick(book) }
            )
        }

        if (state.isLoadingMore) {
            item(key = "loading_more") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.loading),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (!state.hasMore && state.books.isNotEmpty() && !state.isLoadingMore) {
            item(key = "no_more") {
                Text(
                    text = stringResource(Res.string.no_more_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchBookResultItem(
    book: Book,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        SearchBookCover(
            coverUrl = book.coverPath,
            title = book.title,
            modifier = Modifier.size(width = 56.dp, height = 78.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .height(78.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!book.author.isNullOrBlank()) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchFormatTag(book.format)
                if (book.chaptersCount > 0) {
                    Text(
                        text = stringResource(Res.string.chapters_count, book.chaptersCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBookCover(
    coverUrl: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(4.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (coverUrl != null) {
            AsyncImage(
                model = coverUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.take(1),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SearchFormatTag(format: String) {
    val backgroundColor = when (format.lowercase()) {
        "epub" -> FormatEpub
        "pdf" -> FormatPdf
        "txt" -> FormatTxt
        "mobi" -> FormatMobi
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = format.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = backgroundColor
        )
    }
}

@AppVerticalZHPreview
@Composable
private fun SearchBookInitialPreview() {
    AppPreviewContent {
        SearchBookContent(state = SearchBookState())
    }
}

@AppVerticalZHPreview
@Composable
private fun SearchBookLoadingPreview() {
    AppPreviewContent {
        SearchBookContent(
            state = SearchBookState(
                query = "Dune",
                submittedQuery = "Dune",
                isLoading = true,
                hasSearched = true
            )
        )
    }
}

@AppVerticalZHPreview
@Composable
private fun SearchBookEmptyPreview() {
    AppPreviewContent {
        SearchBookContent(
            state = SearchBookState(
                query = "Unknown",
                submittedQuery = "Unknown",
                hasSearched = true
            )
        )
    }
}

@AppVerticalZHPreview
@Composable
private fun SearchBookResultsPreview() {
    AppPreviewContent {
        SearchBookContent(
            state = SearchBookState(
                query = "Dune",
                submittedQuery = "Dune",
                books = listOf(
                    Book(
                        id = 1,
                        title = "Dune",
                        author = "Frank Herbert",
                        format = "epub",
                        filePath = "/books/dune.epub",
                        fileSize = 1024,
                        chaptersCount = 48
                    ),
                    Book(
                        id = 2,
                        title = "Dune Messiah",
                        author = "Frank Herbert",
                        format = "epub",
                        filePath = "/books/dune-messiah.epub",
                        fileSize = 1024,
                        chaptersCount = 24
                    )
                ),
                total = 2,
                hasSearched = true
            )
        )
    }
}
