package com.bookd.app.data.vm

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bookd.app.Database
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.BookApi
import com.bookd.app.data.api.NetworkAddressProvider
import com.bookd.app.data.model.AppBooksResponse
import com.bookd.app.data.model.Book
import com.bookd.app.data.model.BookDetailResponse
import com.bookd.app.data.repository.BookRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchBookViewModelTest {

    private lateinit var fakeApi: SearchBookViewModelFakeBookApi
    private lateinit var viewModel: SearchBookViewModel

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        val database = Database(driver)
        fakeApi = SearchBookViewModelFakeBookApi()
        viewModel = SearchBookViewModel(
            BookRepository(database, SearchBookViewModelFakeApiProvider(fakeApi))
        )
    }

    @Test
    fun `given blank query when submitted then state is cleared and backend is not called`() {
        viewModel.onIntent(SearchBookIntent.QueryChanged("   "))
        viewModel.onIntent(SearchBookIntent.SubmitSearch)

        val state = viewModel.state.value
        assertEquals("", state.submittedQuery)
        assertEquals(emptyList(), state.books)
        assertTrue(state.isInitial)
        assertEquals(emptyList(), fakeApi.searchRequests)
    }

    @Test
    fun `given query when submitted then first page is loaded`() {
        fakeApi.searchResponses[SearchBookViewModelSearchRequest("Dune", null, BookRepository.PAGE_SIZE, 0)] =
            booksResponse(listOf(book(id = 1, title = "Dune")), total = 1, hasMore = false)

        viewModel.onIntent(SearchBookIntent.QueryChanged(" Dune "))
        viewModel.onIntent(SearchBookIntent.SubmitSearch)

        waitUntil { viewModel.state.value.books.map { it.title } == listOf("Dune") }

        val state = viewModel.state.value
        assertEquals("Dune", state.submittedQuery)
        assertEquals(1, state.total)
        assertEquals(false, state.hasMore)
        assertEquals(listOf(SearchBookViewModelSearchRequest("Dune", null, BookRepository.PAGE_SIZE, 0)), fakeApi.searchRequests)
    }

    @Test
    fun `given more results when load more then next page is appended`() {
        fakeApi.searchResponses[SearchBookViewModelSearchRequest("Dune", null, BookRepository.PAGE_SIZE, 0)] =
            booksResponse(listOf(book(id = 1, title = "Dune")), total = 2, hasMore = true)
        fakeApi.searchResponses[SearchBookViewModelSearchRequest("Dune", null, BookRepository.PAGE_SIZE, 1)] =
            booksResponse(listOf(book(id = 2, title = "Dune Messiah")), total = 2, hasMore = false, offset = 1)

        viewModel.onIntent(SearchBookIntent.QueryChanged("Dune"))
        viewModel.onIntent(SearchBookIntent.SubmitSearch)
        waitUntil { viewModel.state.value.hasMore }

        viewModel.onIntent(SearchBookIntent.LoadMore)
        waitUntil { viewModel.state.value.books.map { it.title } == listOf("Dune", "Dune Messiah") }

        assertEquals(false, viewModel.state.value.hasMore)
    }

    @Test
    fun `given different query when submitted then previous results are replaced`() {
        fakeApi.searchResponses[SearchBookViewModelSearchRequest("Dune", null, BookRepository.PAGE_SIZE, 0)] =
            booksResponse(listOf(book(id = 1, title = "Dune")), total = 1, hasMore = false)
        fakeApi.searchResponses[SearchBookViewModelSearchRequest("Foundation", null, BookRepository.PAGE_SIZE, 0)] =
            booksResponse(listOf(book(id = 3, title = "Foundation")), total = 1, hasMore = false)

        viewModel.onIntent(SearchBookIntent.QueryChanged("Dune"))
        viewModel.onIntent(SearchBookIntent.SubmitSearch)
        waitUntil { viewModel.state.value.books.map { it.title } == listOf("Dune") }

        viewModel.onIntent(SearchBookIntent.QueryChanged("Foundation"))
        viewModel.onIntent(SearchBookIntent.SubmitSearch)
        waitUntil { viewModel.state.value.books.map { it.title } == listOf("Foundation") }

        assertEquals("Foundation", viewModel.state.value.submittedQuery)
    }

    @Test
    fun `given result selected then detail navigation effect is emitted`() = runBlocking {
        val effect = async { viewModel.effect.first() }

        viewModel.onIntent(SearchBookIntent.SelectBook(42))

        assertEquals(SearchBookEffect.NavigateToBookDetail(42), effect.await())
    }

    private fun waitUntil(predicate: () -> Boolean) = runBlocking {
        withTimeout(2_000) {
            while (!predicate()) {
                delay(10)
            }
        }
    }

    private fun book(id: Int, title: String): Book =
        Book(
            id = id,
            title = title,
            author = null,
            format = "epub",
            filePath = "/books/$id.epub",
            fileSize = 1024
        )

    private fun booksResponse(
        books: List<Book>,
        total: Int,
        hasMore: Boolean,
        offset: Long = 0
    ): AppBooksResponse =
        AppBooksResponse(
            books = books,
            total = total,
            limit = BookRepository.PAGE_SIZE,
            offset = offset,
            hasMore = hasMore
        )
}

private data class SearchBookViewModelSearchRequest(
    val query: String,
    val sourceId: Int?,
    val limit: Int,
    val offset: Long
)

private class SearchBookViewModelFakeBookApi : BookApi {
    val searchRequests = mutableListOf<SearchBookViewModelSearchRequest>()
    val searchResponses = mutableMapOf<SearchBookViewModelSearchRequest, AppBooksResponse>()

    override suspend fun getBooks(sourceId: Int, limit: Int, offset: Long): AppBooksResponse =
        throw UnsupportedOperationException()

    override suspend fun searchBooks(query: String, sourceId: Int?, limit: Int, offset: Long): AppBooksResponse {
        val request = SearchBookViewModelSearchRequest(query, sourceId, limit, offset)
        searchRequests += request
        return searchResponses.getValue(request)
    }

    override suspend fun getBookDetail(id: Int): BookDetailResponse =
        throw UnsupportedOperationException()
}

private class SearchBookViewModelFakeApiProvider(
    private val bookApi: BookApi
) : ApiProvider(
    httpClient = HttpClient(),
    networkSwitcher = object : NetworkAddressProvider {
        override val currentUrl: String = "http://test"
    }
) {
    override fun getBookApiOrNull(): BookApi = bookApi
}
