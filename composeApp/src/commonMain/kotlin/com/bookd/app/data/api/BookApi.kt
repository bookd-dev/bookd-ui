package com.bookd.app.data.api

import com.bookd.app.data.model.AppBooksResponse
import de.jensklingenberg.ktorfit.http.GET
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
}
