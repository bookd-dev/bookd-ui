package com.bookd.app.data.repository

import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.NoNetworkConfigException
import com.bookd.app.data.model.AddBookToBookshelfRequest
import com.bookd.app.data.model.AddToBookshelvesRequest
import com.bookd.app.data.model.Book
import com.bookd.app.data.model.Bookshelf
import com.bookd.app.data.model.BooksInBookshelfResponse
import com.bookd.app.data.model.CreateBookshelfRequest
import com.bookd.app.data.model.ReorderBookshelvesRequest
import com.bookd.app.data.model.UpdateBookshelfRequest

/**
 * 书架仓库
 * 
 * 负责书架数据的获取和管理
 * 注意：书架数据不做本地缓存，每次从网络获取
 */
class BookshelfRepository(
    private val apiProvider: ApiProvider
) {
    companion object {
        const val PAGE_SIZE = 20
    }
    
    /**
     * 获取用户所有书架
     */
    suspend fun getBookshelves(): Result<List<Bookshelf>> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val bookshelves = api.getBookshelves()
            Result.success(bookshelves)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取书架中的书籍列表
     */
    suspend fun getBooksInBookshelf(
        bookshelfId: Int,
        offset: Long = 0
    ): Result<BooksInBookshelfResponse> {
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookshelfApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val response = api.getBooksInBookshelf(bookshelfId, PAGE_SIZE, offset)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
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
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
