package com.bookd.app.data.model

import kotlinx.serialization.Serializable

/**
 * 阅读进度响应
 */
@Serializable
data class ReadingProgressResponse(
    val id: Int,
    val bookId: Int,
    val progress: Double,
    val currentPage: Int,
    val totalPages: Int? = null,
    val cfiLocation: String? = null,
    val documentId: String? = null,
    val deviceId: String? = null,
    val lastReadAt: String,
    val chapterIndex: Int = currentPage,
    val anchorId: String? = null,
    val paragraphIndex: Int? = null,
    val scrollOffset: Int? = null,
    // 章节详细进度
    val chapterPageIndex: Int? = null,
    val chapterTotalPages: Int? = null,
    val chapterScrollPercent: Double? = null
)

fun ReadingProgressResponse.toReadingPosition(): ReadingPosition = ReadingPosition(
    chapterIndex = chapterIndex,
    anchorId = anchorId,
    paragraphIndex = paragraphIndex ?: 0,
    scrollOffset = scrollOffset ?: 0,
    pageIndex = chapterPageIndex ?: 0,
)

/**
 * 书籍详情响应
 * 
 * 包含书籍完整信息、标签、阅读进度和所在书架信息
 */
@Serializable
data class BookDetailResponse(
    val book: Book,
    val tags: List<Tag> = emptyList(),
    val readingProgress: ReadingProgressResponse? = null,
    val bookshelves: List<Bookshelf> = emptyList(),
    val inDefaultBookshelf: Boolean = false
)
