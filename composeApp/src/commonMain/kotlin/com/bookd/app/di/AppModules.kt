package com.bookd.app.di

import com.bookd.app.data.repository.NetworkConfigRepository
import com.bookd.app.data.repository.NetworkSwitcher
import com.bookd.app.data.repository.UserRepository
import com.bookd.app.data.vm.BookshelfViewModel
import com.bookd.app.settings
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 网络模块 - Ktor HttpClient 配置
 */
val networkModule = module {
    single { NetworkConfigRepository(get()) }
    single { NetworkSwitcher(get(), get()) }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }
    }

    // 动态 baseUrl 的 HttpClient
    single {
        val switcher = get<NetworkSwitcher>()

        HttpClient {
            install(ContentNegotiation) {
                json(get())
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.INFO
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
            }
            defaultRequest {
                switcher.currentUrl?.let { url(it) }
            }
        }
    }
}

/**
 * 数据仓库模块
 */
val repositoryModule = module {
    single {
        UserRepository(
            settings = get(),
            httpClient = get(),
            baseUrl = "http://localhost:7919"
        )
    }

}

/**
 * ViewModel 模块
 */
val viewModelModule = module {
    viewModel { BookshelfViewModel() }
}

val appNavigation = module {
}

/**
 * 所有模块列表
 */
val appModules = listOf(
    networkModule,
    repositoryModule,
    viewModelModule,
    appNavigation
)
