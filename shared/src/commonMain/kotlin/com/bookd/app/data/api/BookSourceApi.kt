package com.bookd.app.data.api

import com.bookd.app.data.model.BookSource
import de.jensklingenberg.ktorfit.http.GET

/**
 * 书源 API
 */
interface BookSourceApi {
    
    /**
     * 获取所有书源
     */
    @GET("api/app/sources")
    suspend fun getSources(): List<BookSource>
}
