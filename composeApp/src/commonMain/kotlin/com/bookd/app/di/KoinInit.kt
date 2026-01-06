package com.bookd.app.di

import org.koin.core.context.startKoin
import org.koin.core.KoinApplication

/**
 * 初始化 Koin 依赖注入
 */
fun initKoin(appDeclaration: KoinApplication.() -> Unit = {}) = startKoin {
    appDeclaration()
    modules(appModules)
}
