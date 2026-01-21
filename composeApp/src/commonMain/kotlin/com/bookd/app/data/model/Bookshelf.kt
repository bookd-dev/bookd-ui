package com.bookd.app.data.model

import kotlinx.serialization.Serializable

/**
 * 书架数据模型
 */
@Serializable
data class Bookshelf(
    val id: Int,
    val userId: Int,
    val name: String,
    val description: String? = null,
    val sortOrder: Int = 0,
    val bookCount: Int = 0,
    val isSystemDefault: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

/**
 * 创建书架请求
 */
@Serializable
data class CreateBookshelfRequest(
    val name: String,
    val description: String? = null
)

/**
 * 更新书架请求
 */
@Serializable
data class UpdateBookshelfRequest(
    val name: String? = null,
    val description: String? = null
)

/**
 * 调整书架顺序请求
 */
@Serializable
data class ReorderBookshelvesRequest(
    val bookshelfIds: List<Int>
)

/**
 * 添加书籍到书架请求
 */
@Serializable
data class AddBookToBookshelfRequest(
    val bookId: Int
)

/**
 * 批量添加书籍到多个书架请求
 */
@Serializable
data class AddToBookshelvesRequest(
    val bookshelfIds: List<Int>
)

/**
 * 书架中书籍列表响应
 */
@Serializable
data class BooksInBookshelfResponse(
    val books: List<Book>,
    val total: Int,
    val limit: Int,
    val offset: Long,
    val hasMore: Boolean
)
