package com.bookd.app.data.model

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============ 书籍清单模型 ============

/**
 * 书籍清单（目录结构）
 */
@Serializable
data class BookManifest(
    val id: Int,
    val title: String,
    val author: String?,
    val format: String,
    val totalChapters: Int,
    val toc: List<TocItem>,
    val spine: List<Int>,
    val metadata: BookMetadata?
)

/**
 * 目录项
 */
@Serializable
data class TocItem(
    val index: Int,
    val title: String,
    val level: Int = 0,
    val wordCount: Int = 0,
    val imageCount: Int = 0,
    val children: List<TocItem> = emptyList(),
    val readStatus: String = "unread",  // "unread" | "reading" | "read"
    val readProgress: Double = 0.0      // 0.0-1.0
)

/**
 * 书籍元数据
 */
@Serializable
data class BookMetadata(
    val publisher: String? = null,
    val language: String? = null,
    val publishDate: String? = null,
    val description: String? = null,
    val isbn: String? = null
)

// ============ 章节内容模型 ============

/**
 * 章节内容
 */
@Serializable
data class ChapterContent(
    val index: Int,
    val title: String?,
    val elements: List<ContentElement>,
    val prevIndex: Int?,
    val nextIndex: Int?
)

/**
 * 内容元素（sealed class）
 */
@Serializable
sealed class ContentElement {
    abstract val anchorId: String?
    
    @Serializable
    @SerialName("paragraph")
    data class Paragraph(
        val spans: List<TextSpan>,
        override val anchorId: String? = null
    ) : ContentElement()
    
    @Serializable
    @SerialName("heading")
    data class Heading(
        val level: Int,
        val text: String,
        override val anchorId: String? = null
    ) : ContentElement()
    
    @Serializable
    @SerialName("image")
    data class Image(
        val src: String,
        val alt: String? = null,
        val width: Int? = null,
        val height: Int? = null,
        val aspectRatio: Double? = null,  // 宽高比 (width / height)
        override val anchorId: String? = null
    ) : ContentElement()
    
    @Serializable
    @SerialName("quote")
    data class Quote(
        val spans: List<TextSpan>,
        override val anchorId: String? = null
    ) : ContentElement()
    
    @Serializable
    @SerialName("code")
    data class Code(
        val text: String,
        val language: String? = null,
        override val anchorId: String? = null
    ) : ContentElement()
    
    @Serializable
    @SerialName("listBlock")
    data class ListBlock(
        val ordered: Boolean,
        val items: List<ListItem>,
        override val anchorId: String? = null
    ) : ContentElement()
    
    @Serializable
    @SerialName("divider")
    data class Divider(
        override val anchorId: String? = null
    ) : ContentElement()
    
    @Serializable
    @SerialName("footnote")
    data class Footnote(
        val footnoteId: String,
        val footnoteImage: String? = null,  // 脚注图片 URL
        val footnoteSpan: TextSpan? = null,  // 脚注序号（如 [1]）
        val width: Int? = null,  // 图片宽度
        val height: Int? = null,  // 图片高度
        val aspectRatio: Double? = null,  // 宽高比 (width / height)
        val contentSpans: List<TextSpan>,  // 脚注内容文本
        override val anchorId: String? = null
    ) : ContentElement()
}

/**
 * 文本片段（支持富文本）
 */
@Serializable
data class TextSpan(
    val text: String,
    val styles: List<TextStyle> = emptyList(),
    val link: String? = null,
    val footnoteId: String? = null  // 脚注引用 ID
) {}

/**
 * 文本样式
 */
@Serializable
enum class TextStyle {
    BOLD,
    ITALIC,
    UNDERLINE,
    STRIKETHROUGH,
    CODE
}

/**
 * 列表项
 */
@Serializable
data class ListItem(
    val spans: List<TextSpan>
)

// ============ 阅读进度模型 ============

/**
 * 阅读进度 DTO（用于 API 请求/响应）
 */
@Serializable
data class ReadingProgressDTO(
    val id: Int? = null,
    val bookId: Int,
    val progress: Double,
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val cfiLocation: String? = null,
    val documentId: String? = null,
    val deviceId: String? = null,
    val lastReadAt: String? = null,
    val chapterIndex: Int? = null,
    val anchorId: String? = null,
    val paragraphIndex: Int? = null,
    val scrollOffset: Int? = null,
    // 章节详细进度
    val chapterPageIndex: Int? = null,
    val chapterTotalPages: Int? = null,
    val chapterScrollPercent: Double? = null
)

// 注意：ReadingProgressResponse 定义在 BookDetail.kt 中

// ============ 阅读器设置模型 ============

/**
 * 阅读器设置 DTO
 */
@Serializable
data class ReaderSettingsDTO(
    val fontFamily: String = "system-ui",
    val fontSize: Int = 18,
    val fontWeight: Int = 400,
    val lineHeight: Double = 1.8,
    val letterSpacing: Double = 0.0,
    val paragraphSpacing: Int = 16,
    val textAlign: String = "justify",
    val pageMode: String = "scroll",
    val brightness: Int = 100,
    val marginHorizontal: Int = 24,
    val marginVertical: Int = 40,
    val firstLineIndent: Boolean = true,
    val pageAnimationType: String = "native"
)

/**
 * 阅读器设置（前端使用的模型，带枚举）
 */
data class ReaderSettings(
    val fontFamily: String = "system-ui",
    val fontSize: Int = 18,
    val fontWeight: Int = 400,
    val lineHeight: Double = 1.8,
    val letterSpacing: Double = 0.0,
    val paragraphSpacing: Int = 16,
    val textAlign: String = "justify",
    val pageMode: PageMode = PageMode.SCROLL,
    val brightness: Int = 100,
    val marginHorizontal: Int = 24,
    val marginVertical: Int = 40,
    val firstLineIndent: Boolean = true,
    val pageAnimationType: PageAnimationType = PageAnimationType.NATIVE
) {
    /**
     * 转换为 DTO 用于 API 请求
     */
    fun toDTO(): ReaderSettingsDTO = ReaderSettingsDTO(
        fontFamily = fontFamily,
        fontSize = fontSize,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        paragraphSpacing = paragraphSpacing,
        textAlign = textAlign,
        pageMode = pageMode.value,
        brightness = brightness,
        marginHorizontal = marginHorizontal,
        marginVertical = marginVertical,
        firstLineIndent = firstLineIndent,
        pageAnimationType = pageAnimationType.value
    )

    fun getFontFamily(): FontFamily = when (fontFamily) {
        FontFamily.SansSerif.name -> FontFamily.SansSerif
        FontFamily.Serif.name -> FontFamily.Serif
        FontFamily.Monospace.name -> FontFamily.Monospace
        FontFamily.Cursive.name -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    fun getTextAlign(): TextAlign = when {
        textAlign.equals("justify", true) -> TextAlign.Justify
        textAlign.equals("center", true) -> TextAlign.Center
        textAlign.equals("right", true) -> TextAlign.Right
        else -> TextAlign.Left
    }
    
    companion object {
        /**
         * 从 DTO 创建 ReaderSettings
         */
        fun fromDTO(dto: ReaderSettingsDTO): ReaderSettings = ReaderSettings(
            fontFamily = dto.fontFamily,
            fontSize = dto.fontSize,
            fontWeight = dto.fontWeight,
            lineHeight = dto.lineHeight,
            letterSpacing = dto.letterSpacing,
            paragraphSpacing = dto.paragraphSpacing,
            textAlign = dto.textAlign,
            pageMode = PageMode.fromValue(dto.pageMode),
            brightness = dto.brightness,
            marginHorizontal = dto.marginHorizontal,
            marginVertical = dto.marginVertical,
            firstLineIndent = dto.firstLineIndent,
            pageAnimationType = PageAnimationType.fromValue(dto.pageAnimationType)
        )
    }
}

/**
 * 阅读模式
 */
enum class PageMode(val value: String) {
    SCROLL("scroll"),
    PAGE("page");
    
    companion object {
        fun fromValue(value: String): PageMode = 
            entries.find { it.value == value } ?: SCROLL
    }
}

/**
 * 翻页动画类型
 */
enum class PageAnimationType(val value: String) {
    NATIVE("native"),
    REALISTIC("realistic");
    
    companion object {
        fun fromValue(value: String): PageAnimationType = 
            entries.find { it.value == value } ?: NATIVE
    }
}

// ============ 书签模型 ============

/**
 * 书签 DTO（用于创建/更新）
 */
@Serializable
data class BookmarkDTO(
    val chapterIndex: Int,
    val anchorId: String? = null,
    val paragraphIndex: Int? = null,
    val scrollOffset: Int? = null,
    val cfiLocation: String? = null,
    val note: String? = null
)

/**
 * 书签响应
 */
@Serializable
data class BookmarkResponse(
    val id: Int,
    val bookId: Int,
    val chapterIndex: Int = 0,
    val anchorId: String? = null,
    val paragraphIndex: Int? = null,
    val scrollOffset: Int? = null,
    val cfiLocation: String? = null,
    val positionType: String? = null,
    val positionValue: String? = null,
    val documentId: String? = null,
    val title: String? = null,
    val note: String? = null,
    val color: String? = null,
    val createdAt: String,
    val updatedAt: String? = null
)

/**
 * 书签列表响应
 */
@Serializable
data class BookmarksResponse(
    val bookmarks: List<BookmarkResponse>,
    val total: Int
)

// ============ 本地进度模型 ============

/**
 * 本地阅读进度（用于精确恢复位置）
 */
data class LocalReadingProgress(
    val bookId: Int,
    val chapterIndex: Int,
    val anchorId: String? = null,
    val paragraphIndex: Int,
    val scrollOffset: Int = 0,
    val pageIndex: Int = 0,
    val progress: Double,
    val lastReadAt: Long
)
