package com.bookd.app.data.api

import com.bookd.app.data.model.AppBooksResponse
import com.bookd.app.data.model.BookDetailResponse
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.Path
import de.jensklingenberg.ktorfit.http.Query

/**
 * 书籍 API
 */
interface BookApi {
    
    /**
     * 获取书籍列表（分页）
     * 
     * @param sourceId 书源 ID
     * @param limit 每页数量，默认 20
     * @param offset 偏移量，默认 0
     */
    @GET("api/app/books")
    suspend fun getBooks(
        @Query("sourceId") sourceId: Int,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Long = 0
    ): AppBooksResponse
    
    /**
     * 获取书籍详情
     * 
     * 包含书籍完整信息、标签、阅读进度和所在书架信息
     * 
     * @param id 书籍 ID
     */
    @GET("api/books/{id}/detail")
    suspend fun getBookDetail(@Path("id") id: Int): BookDetailResponse
}
