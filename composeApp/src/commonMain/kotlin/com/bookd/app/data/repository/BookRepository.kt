package com.bookd.app.data.repository

import com.bookd.app.Database
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.NoNetworkConfigException
import com.bookd.app.data.model.AppBooksResponse
import com.bookd.app.data.model.Book

/**
 * 书籍仓库
 * 
 * 负责书籍数据的获取和缓存管理
 * 策略：按 sourceId 分组缓存，支持分页加载
 */
class BookRepository(
    private val database: Database,
    private val apiProvider: ApiProvider
) {
    private val queries = database.bookQueries
    
    companion object {
        const val PAGE_SIZE = 20
    }
    
    /**
     * 获取书籍列表（分页，优先从缓存）
     * 
     * @param sourceId 书源 ID
     * @param offset 偏移量
     * @param forceRefresh 是否强制从网络刷新（会清除该 sourceId 的所有缓存）
     * @return 分页响应
     */
    suspend fun getBooks(
        sourceId: Int,
        offset: Long = 0,
        forceRefresh: Boolean = false
    ): Result<AppBooksResponse> {
        // 如果强制刷新，先清除该 sourceId 的缓存
        if (forceRefresh) {
            queries.deleteBySourceId(sourceId.toLong())
        }
        
        // 尝试从缓存获取
        val cached = queries.selectBySourceId(
            sourceId = sourceId.toLong(),
            limit = PAGE_SIZE.toLong(),
            offset = offset
        ).executeAsList()
        
        val cachedTotal = queries.countBySourceId(sourceId.toLong()).executeAsOne().toInt()
        
        // 如果缓存有数据且不是强制刷新，直接返回缓存
        if (cached.isNotEmpty() && !forceRefresh) {
            val books = cached.map { it.toBook() }
            return Result.success(
                AppBooksResponse(
                    books = books,
                    total = cachedTotal,
                    limit = PAGE_SIZE,
                    offset = offset,
                    hasMore = offset + books.size < cachedTotal
                )
            )
        }
        
        // 从网络获取
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val response = api.getBooks(
                sourceId = sourceId,
                limit = PAGE_SIZE,
                offset = offset
            )
            
            // 保存到缓存
            response.books.forEach { book ->
                queries.insertOrReplace(
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
            }
            
            Result.success(response)
        } catch (e: Exception) {
            // 网络失败时尝试返回缓存
            val fallback = queries.selectBySourceId(
                sourceId = sourceId.toLong(),
                limit = PAGE_SIZE.toLong(),
                offset = offset
            ).executeAsList()
            
            if (fallback.isNotEmpty()) {
                val books = fallback.map { it.toBook() }
                val total = queries.countBySourceId(sourceId.toLong()).executeAsOne().toInt()
                Result.success(
                    AppBooksResponse(
                        books = books,
                        total = total,
                        limit = PAGE_SIZE,
                        offset = offset,
                        hasMore = offset + books.size < total
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
    suspend fun loadMore(sourceId: Int, offset: Long): Result<AppBooksResponse> {
        return getBooks(sourceId, offset, forceRefresh = false)
    }
    
    /**
     * 刷新书籍列表（清除缓存并重新加载）
     */
    suspend fun refresh(sourceId: Int): Result<AppBooksResponse> {
        return getBooks(sourceId, offset = 0, forceRefresh = true)
    }
    
    /**
     * 根据 ID 获取书籍
     */
    suspend fun getBookById(id: Int): Book? {
        return queries.selectById(id.toLong()).executeAsOneOrNull()?.toBook()
    }
    
    /**
     * 清除指定 sourceId 的缓存
     */
    suspend fun clearCache(sourceId: Int) {
        queries.deleteBySourceId(sourceId.toLong())
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache() {
        queries.deleteAll()
    }
}

/**
 * 将 SQLDelight 实体转换为数据模型
 */
private fun com.bookd.app.BookEntity.toBook(): Book {
    return Book(
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
}
