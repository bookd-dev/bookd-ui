package com.bookd.app.di

import org.koin.core.module.Module

/**
 * 平台特定模块
 * 
 * 包含需要平台特定实现的依赖，如 DatabaseDriverFactory
 */
expect val platformModule: Module
