package com.bookd.app

/**
 * 应用构建配置
 * 
 * 提供应用版本号等构建时信息
 */
expect object AppBuildConfig {
    /**
     * 应用版本名称，如 "1.0.0"
     */
    val VERSION_NAME: String
}
