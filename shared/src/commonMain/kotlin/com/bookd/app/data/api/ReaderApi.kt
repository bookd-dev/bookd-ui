package com.bookd.app.data.api

import com.bookd.app.data.model.BookManifest
import com.bookd.app.data.model.BookmarkDTO
import com.bookd.app.data.model.BookmarkResponse
import com.bookd.app.data.model.BookmarksResponse
import com.bookd.app.data.model.ChapterContent
import com.bookd.app.data.model.ReaderSettingsDTO
import com.bookd.app.data.model.ReadingProgressDTO
import com.bookd.app.data.model.ReadingProgressResponse
import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.DELETE
import de.jensklingenberg.ktorfit.http.GET
import de.jensklingenberg.ktorfit.http.PATCH
import de.jensklingenberg.ktorfit.http.POST
import de.jensklingenberg.ktorfit.http.PUT
import de.jensklingenberg.ktorfit.http.Path

/**
 * 阅读器 API 接口
 * 
 * 包含书籍内容、阅读进度、书签和阅读器设置的 API
 */
interface ReaderApi {
    
    // ============ 书籍内容 ============
    
    /**
     * 获取书籍清单（目录结构）
     */
    @GET("api/books/{id}/manifest")
    suspend fun getBookManifest(@Path("id") bookId: Int): BookManifest
    
    /**
     * 获取带阅读进度的书籍清单（需要认证）
     */
    @GET("api/books/{id}/manifest-with-progress")
    suspend fun getBookManifestWithProgress(@Path("id") bookId: Int): BookManifest
    
    /**
     * 获取章节内容
     */
    @GET("api/books/{id}/chapters/{index}")
    suspend fun getChapterContent(
        @Path("id") bookId: Int,
        @Path("index") chapterIndex: Int
    ): ChapterContent
    
    // ============ 阅读进度 ============
    
    /**
     * 获取阅读进度
     */
    @GET("api/books/{bookId}/progress")
    suspend fun getReadingProgress(@Path("bookId") bookId: Int): ReadingProgressResponse
    
    /**
     * 更新阅读进度
     */
    @PUT("api/books/{bookId}/progress")
    suspend fun updateReadingProgress(
        @Path("bookId") bookId: Int,
        @Body progress: ReadingProgressDTO
    ): ReadingProgressResponse
    
    // ============ 书签 ============
    
    /**
     * 获取书籍书签列表
     */
    @GET("api/books/{bookId}/bookmarks")
    suspend fun getBookmarks(@Path("bookId") bookId: Int): BookmarksResponse
    
    /**
     * 添加书签
     */
    @POST("api/books/{bookId}/bookmarks")
    suspend fun addBookmark(
        @Path("bookId") bookId: Int,
        @Body bookmark: BookmarkDTO
    ): BookmarkResponse
    
    /**
     * 更新书签
     */
    @PUT("api/bookmarks/{id}")
    suspend fun updateBookmark(
        @Path("id") bookmarkId: Int,
        @Body bookmark: BookmarkDTO
    ): BookmarkResponse
    
    /**
     * 删除书签
     */
    @DELETE("api/bookmarks/{id}")
    suspend fun deleteBookmark(@Path("id") bookmarkId: Int)
    
    // ============ 阅读器设置 ============
    
    /**
     * 获取阅读器设置
     */
    @GET("api/user/reader-settings")
    suspend fun getReaderSettings(): ReaderSettingsDTO
    
    /**
     * 完整更新阅读器设置
     */
    @PUT("api/user/reader-settings")
    suspend fun updateReaderSettings(@Body settings: ReaderSettingsDTO): ReaderSettingsDTO
    
    /**
     * 部分更新阅读器设置
     */
    @PATCH("api/user/reader-settings")
    suspend fun patchReaderSettings(@Body settings: ReaderSettingsDTO): ReaderSettingsDTO
}
