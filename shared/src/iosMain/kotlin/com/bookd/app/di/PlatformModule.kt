package com.bookd.app.di

import coil3.ImageLoader
import coil3.PlatformContext
import com.bookd.app.data.db.DatabaseDriverFactory
import com.bookd.app.data.repository.CacheRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory() }
    single { 
        val imageLoader = ImageLoader(PlatformContext.INSTANCE)
        CacheRepository(imageLoaderProvider = { imageLoader })
    }
}
