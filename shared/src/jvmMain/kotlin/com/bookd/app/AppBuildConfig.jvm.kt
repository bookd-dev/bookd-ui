package com.bookd.app

/**
 * Desktop (JVM) 平台构建配置
 * 
 * 版本号从 build.gradle.kts 中 compose.desktop.packageVersion 获取
 * 当前硬编码，建议后续通过 Gradle 构建时注入
 */
actual object AppBuildConfig {
    actual val VERSION_NAME: String = "1.0.0"
}
