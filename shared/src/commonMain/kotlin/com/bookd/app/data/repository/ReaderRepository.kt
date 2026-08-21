package com.bookd.app.data.repository

import com.bookd.app.Database
import com.bookd.app.LocalReadingProgressEntity
import com.bookd.app.basic.extension.format
import com.bookd.app.basic.reader.data.PageAnchor
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.NoNetworkConfigException
import com.bookd.app.data.model.*
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.serialization.json.Json
import kotlin.time.Clock

/**
 * 阅读器仓库
 * 
 * 负责阅读器相关数据的获取、缓存和同步
 * - 书籍清单和章节内容
 * - 阅读进度（本地 + 远程）
 * - 书签
 * - 阅读器设置
 */
class ReaderRepository(
    private val database: Database,
    private val apiProvider: ApiProvider,
    private val settingsStore: Settings,
) {
    private val chapterCacheQueries = database.chapterCacheQueries
    private val pageAnchorCacheQueries = database.pageAnchorCacheQueries
    private val localProgressQueries = database.localReadingProgressQueries
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    // ============ 书籍清单 ============
    
    /**
     * 获取书籍清单（带阅读进度）
     * 需要认证
     */
    suspend fun getBookManifestWithProgress(bookId: Int): Result<BookManifest> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val manifest = api.getBookManifestWithProgress(bookId)
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 获取书籍清单（无阅读进度）
     */
    suspend fun getBookManifest(bookId: Int): Result<BookManifest> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val manifest = api.getBookManifest(bookId)
            Result.success(manifest)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============ 章节内容 ============
    
    /**
     * 获取章节内容
     * 优先从本地缓存获取，缓存未命中则从网络获取并缓存
     */
    suspend fun getChapterContent(bookId: Int, chapterIndex: Int): Result<ChapterContent> {
        // 1. 尝试从缓存获取
        val cached = chapterCacheQueries.selectByBookAndChapter(
            bookId = bookId.toLong(),
            chapterIndex = chapterIndex.toLong()
        ).executeAsOneOrNull()
        
        if (cached != null) {
            return try {
                val content = json.decodeFromString<ChapterContent>(cached.content)
                if (content.elements.isEmpty()) {
                    // elements 为空视为无效缓存（书籍解析未完成时的历史脏数据），
                    // 删除后重新从网络获取以拿到完整内容。
                    chapterCacheQueries.deleteByBookAndChapter(bookId.toLong(), chapterIndex.toLong())
                    fetchAndCacheChapter(bookId, chapterIndex)
                } else {
                    Result.success(content)
                }
            } catch (e: Exception) {
                // 缓存解析失败，删除并重新获取
                chapterCacheQueries.deleteByBookAndChapter(bookId.toLong(), chapterIndex.toLong())
                fetchAndCacheChapter(bookId, chapterIndex)
            }
        }
        
        // 2. 从网络获取
        return fetchAndCacheChapter(bookId, chapterIndex)
    }
    
    /**
     * 预加载章节（不返回结果，静默缓存）
     */
    suspend fun preloadChapter(bookId: Int, chapterIndex: Int) {
        // 检查是否已缓存
        val cached = chapterCacheQueries.selectByBookAndChapter(
            bookId = bookId.toLong(),
            chapterIndex = chapterIndex.toLong()
        ).executeAsOneOrNull()
        
        if (cached == null) {
            // 静默获取并缓存
            fetchAndCacheChapter(bookId, chapterIndex)
        }
    }
    
    /**
     * 从网络获取章节并缓存
     */
    private suspend fun fetchAndCacheChapter(bookId: Int, chapterIndex: Int): Result<ChapterContent> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val content = api.getChapterContent(bookId, chapterIndex)
            
            // 缓存到本地
            val contentJson = json.encodeToString(content)
            chapterCacheQueries.insertOrReplace(
                bookId = bookId.toLong(),
                chapterIndex = chapterIndex.toLong(),
                content = contentJson,
                cachedAt = Clock.System.now().toEpochMilliseconds()
            )
            
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 清除书籍的章节缓存
     */
    suspend fun clearChapterCache(bookId: Int) {
        chapterCacheQueries.deleteByBookId(bookId.toLong())
    }
    
    /**
     * 清除所有章节缓存
     */
    suspend fun clearAllChapterCache() {
        chapterCacheQueries.deleteAll()
    }
    
    /**
     * 获取缓存统计
     */
    suspend fun getCacheStats(): CacheStats {
        val books = chapterCacheQueries.countBooks().executeAsOne().toInt()
        val chapters = chapterCacheQueries.countChapters().executeAsOne().toInt()
        @Suppress("USELESS_CAST")
        val sizeResult: Any? = chapterCacheQueries.totalSize().executeAsOne()
        val size = when (sizeResult) {
            is Long -> sizeResult
            is Number -> sizeResult.toLong()
            else -> 0L
        }
        return CacheStats(books, chapters, size)
    }
    
    // ============ 阅读进度 ============
    
    /**
     * 获取远程阅读进度
     */
    suspend fun getRemoteProgress(bookId: Int): Result<ReadingProgressResponse> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val progress = api.getReadingProgress(bookId)
            Result.success(progress)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 保存远程阅读进度
     */
    suspend fun saveRemoteProgress(bookId: Int, progress: ReadingProgressDTO): Result<ReadingProgressResponse> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val response = api.updateReadingProgress(bookId, progress)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 将同一个不可变快照发送到云端，避免异步期间重新读取变化中的阅读状态。
     */
    suspend fun syncProgressSnapshot(snapshot: ReadingProgressSnapshot): Result<ReadingProgressResponse> {
        return saveRemoteProgress(snapshot.bookId, snapshot.toDTO())
    }
    
    /**
     * 更新远程阅读进度（便捷方法）
     */
    suspend fun updateRemoteProgress(
        bookId: Int,
        progress: Double,
        currentChapter: Int,
        totalChapters: Int,
        anchorId: String? = null,
        paragraphIndex: Int? = null,
        scrollOffset: Int? = null,
        chapterPageIndex: Int? = null,
        chapterTotalPages: Int? = null,
        chapterScrollPercent: Double? = null
    ): Result<ReadingProgressResponse> {
        val dto = ReadingProgressDTO(
            bookId = bookId,
            progress = progress,
            currentPage = currentChapter,
            totalPages = totalChapters,
            chapterIndex = currentChapter,
            anchorId = anchorId,
            paragraphIndex = paragraphIndex,
            scrollOffset = scrollOffset,
            chapterPageIndex = chapterPageIndex,
            chapterTotalPages = chapterTotalPages,
            chapterScrollPercent = chapterScrollPercent
        )
        return saveRemoteProgress(bookId, dto)
    }
    
    /**
     * 获取本地阅读进度
     */
    fun getLocalProgress(bookId: Int): LocalReadingProgress? {
        return localProgressQueries.selectByBookId(bookId.toLong())
            .executeAsOneOrNull()
            ?.toLocalProgress()
    }
    
    /**
     * 保存本地阅读进度
     */
    suspend fun saveLocalProgress(progress: LocalReadingProgress) {
        localProgressQueries.insertOrReplace(
            bookId = progress.bookId.toLong(),
            chapterIndex = progress.chapterIndex.toLong(),
            anchorId = progress.anchorId,
            paragraphIndex = progress.paragraphIndex.toLong(),
            scrollOffset = progress.scrollOffset.toLong(),
            pageIndex = progress.pageIndex.toLong(),
            progress = progress.progress,
            lastReadAt = progress.lastReadAt
        )
    }

    suspend fun saveLocalProgress(snapshot: ReadingProgressSnapshot) {
        saveLocalProgress(snapshot.toLocalProgress())
    }
    
    /**
     * 删除本地阅读进度
     */
    suspend fun deleteLocalProgress(bookId: Int) {
        localProgressQueries.deleteByBookId(bookId.toLong())
    }
    
    /**
     * 获取最近阅读的书籍 ID 列表
     */
    fun getRecentBookIds(limit: Int = 10): List<Int> {
        return localProgressQueries.selectRecentBookIds(limit.toLong())
            .executeAsList()
            .map { it.toInt() }
    }
    
    // ============ 书签 ============
    
    /**
     * 获取书籍书签列表
     */
    suspend fun getBookmarks(bookId: Int): Result<List<BookmarkResponse>> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val response = api.getBookmarks(bookId)
            Result.success(response.bookmarks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 添加书签
     */
    suspend fun addBookmark(bookId: Int, bookmark: BookmarkDTO): Result<BookmarkResponse> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val response = api.addBookmark(bookId, bookmark)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 添加书签（便捷方法）
     */
    suspend fun addBookmark(
        bookId: Int,
        chapterIndex: Int,
        anchorId: String?,
        paragraphIndex: Int?,
        scrollOffset: Int?,
        note: String?
    ): Result<BookmarkResponse> {
        val bookmark = BookmarkDTO(
            chapterIndex = chapterIndex,
            anchorId = anchorId,
            paragraphIndex = paragraphIndex,
            scrollOffset = scrollOffset,
            note = note
        )
        return addBookmark(bookId, bookmark)
    }
    
    /**
     * 更新书签
     */
    suspend fun updateBookmark(bookmarkId: Int, bookmark: BookmarkDTO): Result<BookmarkResponse> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val response = api.updateBookmark(bookmarkId, bookmark)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 删除书签
     */
    suspend fun deleteBookmark(bookmarkId: Int): Result<Unit> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            api.deleteBookmark(bookmarkId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============ 阅读器设置 ============
    
    /**
     * 获取阅读器设置
     * 本地存在待同步设置时优先返回本地值，避免服务端旧值覆盖用户刚修改的设置。
     */
    suspend fun getReaderSettings(): ReaderSettings {
        val localSettings = getLocalReaderSettings()
        if (localSettings != null && hasPendingReaderSettingsSync()) {
            return localSettings
        }

        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return localSettings ?: ReaderSettings()
            
            val dto = api.getReaderSettings()
            ReaderSettings.fromDTO(dto).also { cacheSyncedReaderSettings(it) }
        } catch (_: Exception) {
            localSettings ?: ReaderSettings()
        }
    }

    /**
     * 同步保存到本地，确保快速退出阅读器时设置不会丢失。
     */
    fun saveLocalReaderSettings(settings: ReaderSettings) {
        settingsStore[KEY_READER_SETTINGS] = json.encodeToString(settings.toDTO())
        settingsStore[KEY_READER_SETTINGS_SYNC_PENDING] = true
    }

    fun hasPendingReaderSettingsSync(): Boolean =
        settingsStore[KEY_READER_SETTINGS_SYNC_PENDING, false] && getLocalReaderSettings() != null
    
    /**
     * 更新阅读器设置
     */
    suspend fun updateReaderSettings(settings: ReaderSettings): Result<ReaderSettings> {
        saveLocalReaderSettings(settings)

        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val dto = api.updateReaderSettings(settings.toDTO())
            val syncedSettings = ReaderSettings.fromDTO(dto)
            if (getLocalReaderSettings() == settings) {
                cacheSyncedReaderSettings(syncedSettings)
            }
            Result.success(syncedSettings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 部分更新阅读器设置
     */
    suspend fun patchReaderSettings(settings: ReaderSettingsDTO): Result<ReaderSettings> {
        return try {
            val api = apiProvider.getReaderApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val dto = api.patchReaderSettings(settings)
            Result.success(ReaderSettings.fromDTO(dto))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getLocalReaderSettings(): ReaderSettings? {
        val encoded = settingsStore.getStringOrNull(KEY_READER_SETTINGS) ?: return null
        return try {
            ReaderSettings.fromDTO(json.decodeFromString<ReaderSettingsDTO>(encoded))
        } catch (_: Exception) {
            settingsStore.remove(KEY_READER_SETTINGS)
            settingsStore.remove(KEY_READER_SETTINGS_SYNC_PENDING)
            null
        }
    }

    private fun cacheSyncedReaderSettings(settings: ReaderSettings) {
        settingsStore[KEY_READER_SETTINGS] = json.encodeToString(settings.toDTO())
        settingsStore[KEY_READER_SETTINGS_SYNC_PENDING] = false
    }


    // ============ PageAnchor 缓存 ============

    /**
     * 构建 PageAnchor 缓存键
     *
     * 将影响分页计算的所有维度拼接成唯一字符串键，用于精确命中缓存。
     * 包含：书籍/章节 ID、阅读器外观设置、视口尺寸、density 以及内容哈希。
     */
    fun buildPageAnchorCacheKey(
        bookId: Int,
        chapterIndex: Int,
        settings: ReaderSettings,
        viewportWidth: Int,
        viewportHeight: Int,
        density: Float,
        elements: List<ContentElement>
    ): String {
        val settingsFingerprint = "${settings.fontSize}|${settings.lineHeight}|${settings.letterSpacing}|${settings.paragraphSpacing}|${settings.firstLineIndent}|${settings.marginHorizontal}|${settings.marginVertical}|${settings.fontFamily}|${settings.fontWeight}"
        val contentHash = elements.fold(elements.size) { acc, el -> acc * 31 + el.hashCode() }.toString()
        return "${bookId}|${chapterIndex}|${settingsFingerprint}|${viewportWidth}x${viewportHeight}@${density}|${contentHash}"
    }

    private companion object {
        const val KEY_READER_SETTINGS = "reader_settings_cache"
        const val KEY_READER_SETTINGS_SYNC_PENDING = "reader_settings_sync_pending"
    }

    /**
     * 读取 PageAnchor 缓存
     *
     * 根据 cacheKey 查询本地数据库，反序列化后返回锚点列表。
     * 解析失败时返回 null，由调用方重新计算。
     */
    suspend fun getPageAnchorsCache(cacheKey: String): List<PageAnchor>? {
        return try {
            val cached = pageAnchorCacheQueries.selectByCacheKey(cacheKey).executeAsOneOrNull()
                ?: return null
            json.decodeFromString<List<PageAnchor>>(cached.anchorsJson)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 写入 PageAnchor 缓存
     *
     * 将锚点列表序列化后存入数据库。当总缓存条数达到 500 条时，
     * 自动删除最旧的 100 条以控制磁盘占用。
     */
    suspend fun savePageAnchorsCache(cacheKey: String, anchors: List<PageAnchor>) {
        val anchorsJson = json.encodeToString<List<PageAnchor>>(anchors)
        val now = Clock.System.now().toEpochMilliseconds()
        database.transaction {
            val total = pageAnchorCacheQueries.countAll().executeAsOne()
            if (total >= 500L) {
                pageAnchorCacheQueries.deleteOldEntries(100L)
            }
            pageAnchorCacheQueries.insertOrReplace(
                cacheKey = cacheKey,
                anchorsJson = anchorsJson,
                cachedAt = now,
                lastAccessedAt = now
            )
        }
    }

    /**
     * 更新 PageAnchor 缓存的最后访问时间（用于 LRU 淘汰）
     */
    suspend fun updateLastAccessedAt(cacheKey: String) {
        pageAnchorCacheQueries.updateLastAccessedAt(
            lastAccessedAt = Clock.System.now().toEpochMilliseconds(),
            cacheKey = cacheKey
        )
    }

    /**
     * 清除所有 PageAnchor 缓存
     */
    suspend fun clearPageAnchorCache() {
        pageAnchorCacheQueries.deleteAll()
    }
}

/**
 * 缓存统计
 */
data class CacheStats(
    val totalBooks: Int,
    val totalChapters: Int,
    val totalSizeBytes: Long
) {
    /**
     * 格式化大小显示
     */
    fun formattedSize(): String {
        return when {
            totalSizeBytes < 1024 -> "${totalSizeBytes}B"
            totalSizeBytes < 1024 * 1024 -> "${totalSizeBytes / 1024}KB"
            else -> "%.1fMB".format(totalSizeBytes / 1024.0 / 1024.0)
        }
    }
}

/**
 * 将 SQLDelight 实体转换为数据模型
 */
private fun LocalReadingProgressEntity.toLocalProgress(): LocalReadingProgress {
    return LocalReadingProgress(
        bookId = bookId.toInt(),
        chapterIndex = chapterIndex.toInt(),
        anchorId = anchorId,
        paragraphIndex = paragraphIndex.toInt(),
        scrollOffset = scrollOffset.toInt(),
        pageIndex = pageIndex.toInt(),
        progress = progress,
        lastReadAt = lastReadAt
    )
}
