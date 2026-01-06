package com.bookd.app.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 网络模块 - Ktor HttpClient 配置
 */
val networkModule = module {
    // TODO: 配置 HttpClient
}

/**
 * 数据仓库模块
 */
val repositoryModule = module {
    // TODO: 注册 Repository
}

/**
 * ViewModel 模块
 */
val viewModelModule = module {
}

/**
 * 所有模块列表
 */
val appModules = listOf(
    networkModule,
    repositoryModule,
    viewModelModule
)
