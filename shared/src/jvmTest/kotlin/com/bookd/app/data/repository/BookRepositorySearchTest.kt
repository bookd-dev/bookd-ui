package com.bookd.app.data.repository

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bookd.app.Database
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.BookApi
import com.bookd.app.data.api.NetworkAddressProvider
import com.bookd.app.data.model.AppBooksResponse
import com.bookd.app.data.model.Book
import com.bookd.app.data.model.BookDetailResponse
import io.ktor.client.HttpClient
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BookRepositorySearchTest {

    private lateinit var database: Database
    private lateinit var fakeApi: SearchFakeBookApi
    private lateinit var repository: BookRepository

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)
        fakeApi = SearchFakeBookApi()
        repository = BookRepository(database, SearchFakeApiProvider(fakeApi))
    }

    @Test
    fun `given blank query when searching then backend is not called and empty response is returned`() = runBlocking {
        val result = repository.searchBooks("   ")

        assertTrue(result.isSuccess)
        assertEquals(emptyList(), result.getOrThrow().books)
        assertEquals(0, result.getOrThrow().total)
        assertEquals(emptyList(), fakeApi.searchRequests)
    }

    @Test
    fun `given network search result when searching then results are cached`() = runBlocking {
        fakeApi.searchResponses[SearchRequest("DUNE", null, BookRepository.PAGE_SIZE, 0)] = booksResponse(
            listOf(book(id = 1, title = "Dune", author = "Frank Herbert", isbn = "9780441172719"))
        )

        val result = repository.searchBooks(" DUNE ")

        assertTrue(result.isSuccess)
        assertEquals(listOf("Dune"), result.getOrThrow().books.map { it.title })
        assertEquals(listOf(SearchRequest("DUNE", null, BookRepository.PAGE_SIZE, 0)), fakeApi.searchRequests)
        assertEquals("Dune", database.bookQueries.selectById(1L).executeAsOne().title)
        assertEquals(1, database.bookQueries.countSearchCached(query = "dune", sourceId = null).executeAsOne())
    }

    @Test
    fun `given cached matching books when network search fails then cached fallback is returned`() = runBlocking {
        fakeApi.searchResponses[SearchRequest("Dune", null, BookRepository.PAGE_SIZE, 0)] = booksResponse(
            listOf(book(id = 2, title = "Dune Messiah", author = "Frank Herbert"))
        )
        assertTrue(repository.searchBooks("Dune").isSuccess)

        fakeApi.searchResponses.clear()
        fakeApi.failSearch = true
        val fallback = repository.searchBooks("dune")

        assertTrue(fallback.isSuccess)
        assertEquals(listOf("Dune Messiah"), fallback.getOrThrow().books.map { it.title })
        assertEquals(1, fallback.getOrThrow().total)
    }

    private fun book(
        id: Int,
        title: String,
        author: String? = null,
        isbn: String? = null,
        sourceId: Int? = null
    ): Book = Book(
        id = id,
        title = title,
        author = author,
        format = "epub",
        filePath = "/books/$id.epub",
        fileSize = 1024,
        isbn = isbn,
        sourceId = sourceId
    )

    private fun booksResponse(books: List<Book>): AppBooksResponse =
        AppBooksResponse(
            books = books,
            total = books.size,
            limit = BookRepository.PAGE_SIZE,
            offset = 0,
            hasMore = false
        )
}

private data class SearchRequest(
    val query: String,
    val sourceId: Int?,
    val limit: Int,
    val offset: Long
)

private class SearchFakeBookApi : BookApi {
    val searchRequests = mutableListOf<SearchRequest>()
    val searchResponses = mutableMapOf<SearchRequest, AppBooksResponse>()
    var failSearch = false

    override suspend fun getBooks(sourceId: Int, limit: Int, offset: Long): AppBooksResponse =
        throw UnsupportedOperationException()

    override suspend fun searchBooks(query: String, sourceId: Int?, limit: Int, offset: Long): AppBooksResponse {
        val request = SearchRequest(query, sourceId, limit, offset)
        searchRequests += request
        if (failSearch) {
            throw RuntimeException("network failed")
        }
        return searchResponses.getValue(request)
    }

    override suspend fun getBookDetail(id: Int): BookDetailResponse =
        throw UnsupportedOperationException()
}

private class SearchFakeApiProvider(
    private val bookApi: BookApi,
    networkConfigured: Boolean = true
) : ApiProvider(
    httpClient = HttpClient(),
    networkSwitcher = object : NetworkAddressProvider {
        override val currentUrl: String? = if (networkConfigured) "http://test" else null
    }
) {
    override fun getBookApiOrNull(): BookApi = bookApi
}
