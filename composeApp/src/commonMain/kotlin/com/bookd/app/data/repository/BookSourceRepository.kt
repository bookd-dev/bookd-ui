package com.bookd.app.data.repository

import com.bookd.app.Database
import com.bookd.app.data.api.ApiProvider
import com.bookd.app.data.api.NoNetworkConfigException
import com.bookd.app.data.model.BookSource

/**
 * 书源仓库
 * 
 * 负责书源数据的获取和缓存管理
 * 策略：完全缓存，手动刷新时清除并重新加载
 */
class BookSourceRepository(
    private val database: Database,
    private val apiProvider: ApiProvider
) {
    private val queries = database.bookSourceQueries
    
    /**
     * 获取所有书源（优先从缓存）
     * 
     * @param forceRefresh 是否强制从网络刷新
     * @return 书源列表
     */
    suspend fun getSources(forceRefresh: Boolean = false): Result<List<BookSource>> {
        // 如果强制刷新，先清除缓存
        if (forceRefresh) {
            queries.deleteAll()
        }
        
        // 尝试从缓存获取
        val cached = queries.selectAll().executeAsList()
        if (cached.isNotEmpty() && !forceRefresh) {
            return Result.success(cached.map { it.toBookSource() })
        }
        
        // 从网络获取
        if (!apiProvider.isConfigured) {
            return Result.failure(NoNetworkConfigException())
        }
        
        return try {
            val api = apiProvider.getBookSourceApiOrNull()
                ?: return Result.failure(NoNetworkConfigException())
            
            val sources = api.getSources()
            
            // 保存到缓存
            sources.forEach { source ->
                queries.insertOrReplace(
                    id = source.id.toLong(),
                    name = source.name,
                    path = source.path,
                    enabled = if (source.enabled) 1L else 0L
                )
            }
            
            Result.success(sources)
        } catch (e: Exception) {
            // 网络失败时尝试返回缓存
            val fallback = queries.selectAll().executeAsList()
            if (fallback.isNotEmpty()) {
                Result.success(fallback.map { it.toBookSource() })
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * 根据 ID 获取书源
     */
    suspend fun getSourceById(id: Int): BookSource? {
        return queries.selectById(id.toLong()).executeAsOneOrNull()?.toBookSource()
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearCache() {
        queries.deleteAll()
    }
}

/**
 * 将 SQLDelight 实体转换为数据模型
 */
private fun com.bookd.app.BookSourceEntity.toBookSource(): BookSource {
    return BookSource(
        id = id.toInt(),
        name = name,
        path = path,
        enabled = enabled == 1L
    )
}
