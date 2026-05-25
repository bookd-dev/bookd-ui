package com.bookd.app.data.vm

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bookd.app.Database
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.BookApi
import com.bookd.app.data.api.BookSourceApi
import com.bookd.app.data.api.NetworkAddressProvider
import com.bookd.app.data.model.AppBooksResponse
import com.bookd.app.data.model.Book
import com.bookd.app.data.model.BookDetailResponse
import com.bookd.app.data.model.BookSource
import com.bookd.app.data.repository.BookRepository
import com.bookd.app.data.repository.BookSourceRepository
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class BookSourceViewModelRefreshAllTest {

    private lateinit var database: Database
    private lateinit var sourceApi: FakeBookSourceApi
    private lateinit var bookApi: FakeBookApi
    private lateinit var viewModel: BookSourceViewModel

    @Before
    fun setUp() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.synchronous().create(driver)
        database = Database(driver)

        sourceApi = FakeBookSourceApi()
        bookApi = FakeBookApi()
        val apiProvider = FakeApiProvider(sourceApi, bookApi)
        viewModel = BookSourceViewModel(
            bookSourceRepository = BookSourceRepository(database, apiProvider),
            bookRepository = BookRepository(database, apiProvider)
        )
    }

    @Test
    fun `given multiple sources when refresh all then refreshes every source and updates local state`() {
        val sources = listOf(
            bookSource(id = 1, name = "Source A"),
            bookSource(id = 2, name = "Source B")
        )
        val sourceOneBooks = listOf(book(id = 101, sourceId = 1, title = "Fresh A"))
        val sourceTwoBooks = listOf(
            book(id = 201, sourceId = 2, title = "Fresh B1"),
            book(id = 202, sourceId = 2, title = "Fresh B2")
        )
        sourceApi.sources = sources
        bookApi.responses = mapOf(
            1 to booksResponse(sourceOneBooks),
            2 to booksResponse(sourceTwoBooks)
        )

        viewModel.onIntent(BookSourceIntent.RefreshAll)

        waitUntil {
            val state = viewModel.state.value
            state.sources == sources &&
                state.booksBySource[1]?.map { it.title } == listOf("Fresh A") &&
                state.booksBySource[2]?.map { it.title } == listOf("Fresh B1", "Fresh B2") &&
                !state.sourcesLoading &&
                state.booksLoading.values.none { it } &&
                state.booksRefreshing.values.none { it }
        }

        assertEquals(1, sourceApi.callCount)
        assertEquals(listOf(1, 2), bookApi.requestedSourceIds)
        assertEquals(2, database.bookSourceQueries.count().executeAsOne())
        assertEquals(1, database.bookQueries.countBySourceId(1).executeAsOne())
        assertEquals(2, database.bookQueries.countBySourceId(2).executeAsOne())
    }

    private fun waitUntil(predicate: () -> Boolean) = runBlocking {
        withTimeout(2_000) {
            while (!predicate()) {
                delay(10)
            }
        }
    }

    private fun bookSource(id: Int, name: String): BookSource =
        BookSource(
            id = id,
            name = name,
            path = "/source/$id",
            enabled = true
        )

    private fun book(id: Int, sourceId: Int, title: String): Book =
        Book(
            id = id,
            title = title,
            author = null,
            format = "epub",
            filePath = "/books/$id.epub",
            fileSize = 1024,
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

private class FakeBookSourceApi : BookSourceApi {
    var sources: List<BookSource> = emptyList()
    var callCount = 0

    override suspend fun getSources(): List<BookSource> {
        callCount++
        return sources
    }
}

private class FakeBookApi : BookApi {
    var responses: Map<Int, AppBooksResponse> = emptyMap()
    val requestedSourceIds = mutableListOf<Int>()

    override suspend fun getBooks(sourceId: Int, limit: Int, offset: Long): AppBooksResponse {
        requestedSourceIds += sourceId
        return responses.getValue(sourceId)
    }

    override suspend fun getBookDetail(id: Int): BookDetailResponse =
        throw UnsupportedOperationException()
}

private class FakeApiProvider(
    private val sourceApi: BookSourceApi,
    private val bookApi: BookApi
) : ApiProvider(
    httpClient = HttpClient(),
    networkSwitcher = object : NetworkAddressProvider {
        override val currentUrl: String = "http://test"
    }
) {
    override fun getBookSourceApiOrNull(): BookSourceApi = sourceApi

    override fun getBookApiOrNull(): BookApi = bookApi
}
