package com.bookd.app.di

import com.bookd.app.data.api.AuthApi
import com.bookd.app.data.api.HealthApi
import com.bookd.app.data.api.createAuthApi
import com.bookd.app.data.api.createHealthApi
import com.bookd.app.data.auth.DefaultHeaderProvider
import com.bookd.app.data.repository.NetworkConfigRepository
import com.bookd.app.data.repository.NetworkSwitcher
import com.bookd.app.data.repository.UserRepository
import com.bookd.app.data.vm.BookshelfViewModel
import com.bookd.app.settings
import de.jensklingenberg.ktorfit.Ktorfit
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * 网络模块 - Ktor HttpClient 配置
 */
val networkModule = module {
    single { NetworkConfigRepository(settings) }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = false
        }
    }
    
    // Header 提供者 - 从 UserRepository 获取 token
    single {
        DefaultHeaderProvider { getOrNull<UserRepository>() }
    }

    // 动态 baseUrl 的 HttpClient
    single {
        val headerProvider = get<DefaultHeaderProvider>()

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
                // 自动添加所有 Headers
                headerProvider.getHeaders().forEach { (key, value) ->
                    header(key, value)
                }
            }
        }
    }
    
    // NetworkSwitcher
    single { NetworkSwitcher(get(), get()) }
    
    // Ktorfit 实例 (动态 baseUrl)
    single {
        val switcher = get<NetworkSwitcher>()
        Ktorfit.Builder()
            .baseUrl(switcher.currentUrl ?: "")
            .httpClient(get<HttpClient>())
            .build()
    }
    
    // API 接口
    single<AuthApi> { get<Ktorfit>().createAuthApi() }
    single<HealthApi> { get<Ktorfit>().createHealthApi() }
}

/**
 * 数据仓库模块
 */
val repositoryModule = module {
    single {
        UserRepository(
            settings = settings,
            authApi = get(),
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
