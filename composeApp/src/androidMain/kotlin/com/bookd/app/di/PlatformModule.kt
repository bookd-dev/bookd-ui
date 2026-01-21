package com.bookd.app.di

import coil3.ImageLoader
import coil3.SingletonImageLoader
import com.bookd.app.BookdApplication
import com.bookd.app.data.db.DatabaseDriverFactory
import com.bookd.app.data.repository.CacheRepository
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(BookdApplication.instance) }
    single { 
        CacheRepository(
            imageLoaderProvider = { SingletonImageLoader.get(BookdApplication.instance) }
        )
    }
}
