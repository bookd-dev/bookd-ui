package com.bookd.app.di

import com.bookd.app.BookdApplication
import com.bookd.app.data.db.DatabaseDriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single { DatabaseDriverFactory(BookdApplication.instance) }
}
