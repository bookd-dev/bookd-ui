package com.bookd.app.data.repository

import coil3.ImageLoader
import coil3.PlatformContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * 缓存管理仓库
 * 
 * 负责管理应用缓存，主要是 Coil 图片缓存
 */
class CacheRepository(
    private val imageLoaderProvider: () -> ImageLoader,
) {
    
    /**
     * 获取缓存大小（字节）
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            val imageLoader = imageLoaderProvider()
            imageLoader.diskCache?.size ?: 0L
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearCache(): Unit = withContext(Dispatchers.IO) {
        try {
            val imageLoader = imageLoaderProvider()
            imageLoader.memoryCache?.clear()
            imageLoader.diskCache?.clear()
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
