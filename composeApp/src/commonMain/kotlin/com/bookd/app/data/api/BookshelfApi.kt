package com.bookd.app.data.api

import com.bookd.app.data.model.AddBookToBookshelfRequest
import com.bookd.app.data.model.AddToBookshelvesRequest
import com.bookd.app.data.model.Bookshelf
import com.bookd.app.data.model.BooksInBookshelfResponse
import com.bookd.app.data.model.CreateBookshelfRequest
import com.bookd.app.data.model.RemoveFromBookshelvesRequest
import com.bookd.app.data.model.ReorderBookshelvesRequest
import com.bookd.app.data.model.UpdateBookshelfRequest
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

/**
 * 书架 API 接口
 */
interface BookshelfApi {
    
    /**
     * 获取用户所有书架
     */
    @GET("api/bookshelves")
    suspend fun getBookshelves(): List<Bookshelf>
    
    /**
     * 创建新书架
     */
    @POST("api/bookshelves")
    suspend fun createBookshelf(@Body request: CreateBookshelfRequest): Bookshelf
    
    /**
     * 更新书架信息
     */
    @PUT("api/bookshelves/{id}")
    suspend fun updateBookshelf(
        @Path("id") id: Int,
        @Body request: UpdateBookshelfRequest
    ): Bookshelf
    
    /**
     * 删除书架
     */
    @DELETE("api/bookshelves/{id}")
    suspend fun deleteBookshelf(@Path("id") id: Int)
    
    /**
     * 调整书架顺序
     */
    @PUT("api/bookshelves/reorder")
    suspend fun reorderBookshelves(@Body request: ReorderBookshelvesRequest)
    
    /**
     * 获取书架中的书籍列表
     */
    @GET("api/bookshelves/{id}/books")
    suspend fun getBooksInBookshelf(
        @Path("id") id: Int,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Long = 0
    ): BooksInBookshelfResponse
    
    /**
     * 添加书籍到书架
     */
    @POST("api/bookshelves/{id}/books")
    suspend fun addBookToBookshelf(
        @Path("id") id: Int,
        @Body request: AddBookToBookshelfRequest
    )
    
    /**
     * 从书架移除书籍
     */
    @DELETE("api/bookshelves/{id}/books/{bookId}")
    suspend fun removeBookFromBookshelf(
        @Path("id") id: Int,
        @Path("bookId") bookId: Int
    )
    
    /**
     * 获取书籍所在的书架列表
     */
    @GET("api/books/{bookId}/bookshelves")
    suspend fun getBookshelvesForBook(@Path("bookId") bookId: Int): List<Bookshelf>
    
    /**
     * 批量添加书籍到多个书架
     */
    @POST("api/books/{bookId}/bookshelves")
    suspend fun addBookToBookshelves(
        @Path("bookId") bookId: Int,
        @Body request: AddToBookshelvesRequest
    )
    
    /**
     * 批量从多个书架移除书籍
     */
    @DELETE("api/books/{bookId}/bookshelves")
    suspend fun removeBookFromBookshelves(
        @Path("bookId") bookId: Int,
        @Body request: RemoveFromBookshelvesRequest
    )
}
