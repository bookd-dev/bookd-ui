package com.bookd.app.data.model

import kotlinx.serialization.Serializable

/**
 * 书籍数据模型
 * 
 * 对应后端 Book 实体，封面路径已由后端拼接为完整 URL
 */
@Serializable
data class Book(
    val id: Int,
    val title: String,
    val author: String? = null,
    val format: String,
    val filePath: String,
    val fileSize: Long,
    val coverPath: String? = null,
    val isbn: String? = null,
    val publisher: String? = null,
    val description: String? = null,
    val sourceId: Int? = null,
    val chapterCount: Int = 0,
    val totalWordCount: Int = 0,
    val totalImageCount: Int = 0,
    val chaptersParsed: Boolean = false,
    val chaptersCount: Int = 0,
    val lastParsedAt: String? = null,
    val parseStatus: String? = null,
    val parseProgress: Int = 0,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/**
 * APP 书籍列表响应
 * 
 * 分页响应，包含书籍列表和分页信息
 */
@Serializable
data class AppBooksResponse(
    val books: List<Book>,
    val total: Int,
    val limit: Int,
    val offset: Long,
    val hasMore: Boolean
)
