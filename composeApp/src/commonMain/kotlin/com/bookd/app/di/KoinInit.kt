package com.bookd.app.di

import org.koin.dsl.KoinConfiguration
import org.koin.dsl.koinConfiguration

/**
 * 初始化 Koin 依赖注入
 */
fun initKoin(): KoinConfiguration = koinConfiguration {
    modules(appModules)
}