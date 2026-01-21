package com.bookd.app.data.repository

import com.bookd.app.BookEntity
import com.bookd.app.BookshelfEntity
import com.bookd.app.Database
import com.bookd.app.SelectBooksInBookshelf
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.NoNetworkConfigException
import com.bookd.app.data.model.AddBookToBookshelfRequest
import com.bookd.app.data.model.AddToBookshelvesRequest
import com.bookd.app.data.model.Book
import com.bookd.app.data.model.Bookshelf
import com.bookd.app.data.model.BookWithProgress
import com.bookd.app.data.model.BooksInBookshelfResponse
import com.bookd.app.data.model.CreateBookshelfRequest
import com.bookd.app.data.model.ReadingProgressResponse
import com.bookd.app.data.model.RemoveFromBookshelvesRequest
import com.bookd.app.data.model.ReorderBookshelvesRequest
import com.bookd.app.data.model.UpdateBookshelfRequest

/**
 * 书架仓库
 * 
 * 负责书架数据的获取和缓存管理
 * 策略：
 * - 书架列表：本地缓存优先，每次进入页面后台同步网络数据
 * - 书架书籍：按 bookshelfId 分组缓存，支持分页加载
 */
class BookshelfRepository(
    private val database: Database,
    private val apiProvider: ApiProvider
) {
    private val bookshelfQueries = database.bookshelfQueries
    private val bookQueries = database.bookQueries
    
    companion object {
        const val PAGE_SIZE = 20
    }
    
    // ==================== 书架列表 ====================
    
    /**
     * 获取书架列表（优先从缓存）
     * 
     * @param forceRefresh 是否强制从网络刷新
     */
    suspend fun getBookshelves(forceRefresh: Boolean = false): Result<List<Bookshelf>> {
        // 如果强制刷新，先尝试从网络获取
        if (forceRefresh) {
            val networkResult = fetchBookshelvesFromNetwork()
            if (networkResult.isSuccess) {
                return networkResult
            }
            // 网络失败时回退到缓存
        }
        
        // 尝试从缓存获取
        val cached = bookshelfQueries.selectAll().executeAsList()
        if (cached.isNotEmpty() && !forceRefresh) {
            return Result.success(cached.map { it.toBookshelf() })
        }
        
        // 缓存为空或强制刷新，从网络获取
        return fetchBookshelvesFromNetwork()
    }
    
    /**
     * 获取缓存的书架列表（仅本地）
     */
    fun getCachedBookshelves(): List<Bookshelf> {
        return bookshelfQueries.selectAll().executeAsList().map { it.toBookshelf() }
    }
    
    /**
     * 同步书架列表到本地（后台调用）
     */
    suspend fun syncBookshelves(): Result<List<Bookshelf>> {
        return fetchBookshelvesFromNetwork()
    }
    
    private suspend fun fetchBookshelvesFromNetwork(): Result<List<Bookshelf>> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val bookshelves = api.getBookshelves()
            
            // 清除旧缓存并保存新数据
            bookshelfQueries.deleteAll()
            bookshelves.forEach { bookshelf ->
                bookshelfQueries.insertOrReplace(
                    id = bookshelf.id.toLong(),
                    userId = bookshelf.userId.toLong(),
                    name = bookshelf.name,
                    description = bookshelf.description,
                    sortOrder = bookshelf.sortOrder.toLong(),
                    bookCount = bookshelf.bookCount.toLong(),
                    isSystemDefault = if (bookshelf.isSystemDefault) 1L else 0L,
                    createdAt = bookshelf.createdAt,
                    updatedAt = bookshelf.updatedAt
                )
            }
            
            Result.success(bookshelves)
        } catch (e: Exception) {
            // 网络失败时尝试返回缓存
            val cached = bookshelfQueries.selectAll().executeAsList()
            if (cached.isNotEmpty()) {
                Result.success(cached.map { it.toBookshelf() })
            } else {
                Result.failure(e)
            }
        }
    }
    
    // ==================== 书架书籍 ====================
    
    /**
     * 获取书架中的书籍列表（分页，优先从缓存）
     * 
     * @param bookshelfId 书架ID
     * @param offset 偏移量
     * @param forceRefresh 是否强制从网络刷新
     */
    suspend fun getBooksInBookshelf(
        bookshelfId: Int,
        offset: Long = 0,
        forceRefresh: Boolean = false
    ): Result<BooksInBookshelfResponse> {
        // 如果强制刷新，清除该书架的缓存
        if (forceRefresh) {
            bookshelfQueries.deleteBooksByBookshelfId(bookshelfId.toLong())
        }
        
        // 尝试从缓存获取
        val cached = bookshelfQueries.selectBooksInBookshelf(
            bookshelfId = bookshelfId.toLong(),
            limit = PAGE_SIZE.toLong(),
            offset = offset
        ).executeAsList()
        
        val cachedTotal = bookshelfQueries.countBooksInBookshelf(bookshelfId.toLong())
            .executeAsOne().toInt()
        
        // 如果缓存有数据且不是强制刷新，直接返回缓存
        if (cached.isNotEmpty() && !forceRefresh) {
            val booksWithProgress = cached.map { it.toBookWithProgress() }
            return Result.success(
                BooksInBookshelfResponse(
                    books = booksWithProgress,
                    total = cachedTotal,
                    limit = PAGE_SIZE,
                    offset = offset,
                    hasMore = offset + booksWithProgress.size < cachedTotal
                )
            )
        }
        
        // 从网络获取
        return fetchBooksInBookshelfFromNetwork(bookshelfId, offset)
    }
    
    /**
     * 同步书架书籍到本地（后台调用）
     */
    suspend fun syncBooksInBookshelf(bookshelfId: Int): Result<BooksInBookshelfResponse> {
        // 清除该书架的缓存
        bookshelfQueries.deleteBooksByBookshelfId(bookshelfId.toLong())
        // 从网络获取第一页
        return fetchBooksInBookshelfFromNetwork(bookshelfId, 0)
    }
    
    private suspend fun fetchBooksInBookshelfFromNetwork(
        bookshelfId: Int,
        offset: Long
    ): Result<BooksInBookshelfResponse> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val response = api.getBooksInBookshelf(bookshelfId, PAGE_SIZE, offset)
            
            // 保存到缓存
            response.books.forEachIndexed { index, bookWithProgress ->
                val book = bookWithProgress.book
                val progress = bookWithProgress.progress
                
                // 保存书籍信息到 BookEntity
                bookQueries.insertOrReplace(
                    id = book.id.toLong(),
                    title = book.title,
                    author = book.author,
                    format = book.format,
                    filePath = book.filePath,
                    fileSize = book.fileSize,
                    coverPath = book.coverPath,
                    isbn = book.isbn,
                    publisher = book.publisher,
                    description = book.description,
                    sourceId = book.sourceId?.toLong(),
                    chapterCount = book.chapterCount.toLong(),
                    totalWordCount = book.totalWordCount.toLong(),
                    totalImageCount = book.totalImageCount.toLong(),
                    chaptersParsed = if (book.chaptersParsed) 1L else 0L,
                    chaptersCount = book.chaptersCount.toLong(),
                    lastParsedAt = book.lastParsedAt,
                    parseStatus = book.parseStatus,
                    parseProgress = book.parseProgress.toLong(),
                    createdAt = book.createdAt,
                    updatedAt = book.updatedAt
                )
                
                // 保存书架-书籍关联（带进度）
                bookshelfQueries.insertOrReplaceBookshelfBook(
                    bookshelfId = bookshelfId.toLong(),
                    bookId = book.id.toLong(),
                    progressId = progress?.id?.toLong(),
                    progress = progress?.progress,
                    currentPage = progress?.currentPage?.toLong(),
                    totalPages = progress?.totalPages?.toLong(),
                    lastReadAt = progress?.lastReadAt,
                    sortIndex = (offset + index).toLong()  // 保持服务器返回的排序
                )
            }
            
            Result.success(response)
        } catch (e: Exception) {
            // 网络失败时尝试返回缓存
            val fallback = bookshelfQueries.selectBooksInBookshelf(
                bookshelfId = bookshelfId.toLong(),
                limit = PAGE_SIZE.toLong(),
                offset = offset
            ).executeAsList()
            
            if (fallback.isNotEmpty()) {
                val booksWithProgress = fallback.map { it.toBookWithProgress() }
                val total = bookshelfQueries.countBooksInBookshelf(bookshelfId.toLong())
                    .executeAsOne().toInt()
                Result.success(
                    BooksInBookshelfResponse(
                        books = booksWithProgress,
                        total = total,
                        limit = PAGE_SIZE,
                        offset = offset,
                        hasMore = offset + booksWithProgress.size < total
                    )
                )
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 加载更多书籍（追加到缓存）
     */
    suspend fun loadMoreBooks(bookshelfId: Int, offset: Long): Result<BooksInBookshelfResponse> {
        return getBooksInBookshelf(bookshelfId, offset, forceRefresh = false)
    }
    
    // ==================== 书架管理 ====================
    
    /**
     * 创建新书架
     */
    suspend fun createBookshelf(name: String, description: String? = null): Result<Bookshelf> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val bookshelf = api.createBookshelf(CreateBookshelfRequest(name, description))
            
            // 添加到本地缓存
            bookshelfQueries.insertOrReplace(
                id = bookshelf.id.toLong(),
                userId = bookshelf.userId.toLong(),
                name = bookshelf.name,
                description = bookshelf.description,
                sortOrder = bookshelf.sortOrder.toLong(),
                bookCount = bookshelf.bookCount.toLong(),
                isSystemDefault = if (bookshelf.isSystemDefault) 1L else 0L,
                createdAt = bookshelf.createdAt,
                updatedAt = bookshelf.updatedAt
            )
            
            Result.success(bookshelf)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 更新书架信息
     */
    suspend fun updateBookshelf(
        id: Int,
        name: String? = null,
        description: String? = null
    ): Result<Bookshelf> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val bookshelf = api.updateBookshelf(id, UpdateBookshelfRequest(name, description))
            
            // 更新本地缓存
            bookshelfQueries.insertOrReplace(
                id = bookshelf.id.toLong(),
                userId = bookshelf.userId.toLong(),
                name = bookshelf.name,
                description = bookshelf.description,
                sortOrder = bookshelf.sortOrder.toLong(),
                bookCount = bookshelf.bookCount.toLong(),
                isSystemDefault = if (bookshelf.isSystemDefault) 1L else 0L,
                createdAt = bookshelf.createdAt,
                updatedAt = bookshelf.updatedAt
            )
            
            Result.success(bookshelf)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除书架
     */
    suspend fun deleteBookshelf(id: Int): Result<Unit> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            api.deleteBookshelf(id)
            
            // 从本地缓存删除
            bookshelfQueries.deleteBooksByBookshelfId(id.toLong())
            bookshelfQueries.deleteById(id.toLong())
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 调整书架顺序
     */
    suspend fun reorderBookshelves(bookshelfIds: List<Int>): Result<Unit> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            api.reorderBookshelves(ReorderBookshelvesRequest(bookshelfIds))
            
            // 重新同步书架列表以更新本地缓存的排序
            syncBookshelves()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 书籍操作 ====================
    
    /**
     * 添加书籍到书架
     */
    suspend fun addBookToBookshelf(bookshelfId: Int, bookId: Int): Result<Unit> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            api.addBookToBookshelf(bookshelfId, AddBookToBookshelfRequest(bookId))
            
            // 重新同步该书架的书籍
            syncBooksInBookshelf(bookshelfId)
            // 同时同步书架列表（更新 bookCount）
            syncBookshelves()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 从书架移除书籍
     */
    suspend fun removeBookFromBookshelf(bookshelfId: Int, bookId: Int): Result<Unit> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            api.removeBookFromBookshelf(bookshelfId, bookId)
            
            // 重新同步该书架的书籍
            syncBooksInBookshelf(bookshelfId)
            // 同时同步书架列表（更新 bookCount）
            syncBookshelves()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取书籍所在的书架列表
     */
    suspend fun getBookshelvesForBook(bookId: Int): Result<List<Bookshelf>> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val bookshelves = api.getBookshelvesForBook(bookId)
            Result.success(bookshelves)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 批量添加书籍到多个书架
     */
    suspend fun addBookToBookshelves(bookId: Int, bookshelfIds: List<Int>): Result<Unit> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            api.addBookToBookshelves(bookId, AddToBookshelvesRequest(bookshelfIds))
            
            // 同步书架列表（更新 bookCount）
            syncBookshelves()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 批量从多个书架移除书籍
     */
    suspend fun removeBookFromBookshelves(bookId: Int, bookshelfIds: List<Int>): Result<Unit> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            api.removeBookFromBookshelves(bookId, RemoveFromBookshelvesRequest(bookshelfIds))
            
            // 同步书架列表（更新 bookCount）
            syncBookshelves()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 清除所有书架缓存
     */
    suspend fun clearAllCache() {
        bookshelfQueries.deleteAll()
        bookshelfQueries.deleteAllBooks()
    }
    
    /**
     * 清除指定书架的书籍缓存
     */
    suspend fun clearBookshelfBooksCache(bookshelfId: Int) {
        bookshelfQueries.deleteBooksByBookshelfId(bookshelfId.toLong())
    }
}

/**
 * 将 SQLDelight 实体转换为数据模型
 */
private fun BookshelfEntity.toBookshelf(): Bookshelf {
    return Bookshelf(
        id = id.toInt(),
        userId = userId.toInt(),
        name = name,
        description = description,
        sortOrder = sortOrder.toInt(),
        bookCount = bookCount.toInt(),
        isSystemDefault = isSystemDefault == 1L,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * 将联表查询结果转换为 BookWithProgress
 */
private fun SelectBooksInBookshelf.toBookWithProgress(): BookWithProgress {
    val book = Book(
        id = id.toInt(),
        title = title,
        author = author,
        format = format,
        filePath = filePath,
        fileSize = fileSize,
        coverPath = coverPath,
        isbn = isbn,
        publisher = publisher,
        description = description,
        sourceId = sourceId?.toInt(),
        chapterCount = chapterCount.toInt(),
        totalWordCount = totalWordCount.toInt(),
        totalImageCount = totalImageCount.toInt(),
        chaptersParsed = chaptersParsed == 1L,
        chaptersCount = chaptersCount.toInt(),
        lastParsedAt = lastParsedAt,
        parseStatus = parseStatus,
        parseProgress = parseProgress.toInt(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
    
    val readingProgress = if (progressId != null) {
        ReadingProgressResponse(
            id = progressId.toInt(),
            bookId = bookId.toInt(),
            progress = progress ?: 0.0,
            currentPage = currentPage?.toInt() ?: 0,
            totalPages = totalPages?.toInt(),
            cfiLocation = null,
            documentId = null,
            deviceId = null,
            lastReadAt = lastReadAt ?: ""
        )
    } else {
        null
    }
    
    return BookWithProgress(
        book = book,
        progress = readingProgress
    )
}
