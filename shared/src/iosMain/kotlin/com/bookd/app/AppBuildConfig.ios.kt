package com.bookd.app

import platform.Foundation.NSBundle

/**
 * iOS 平台构建配置
 */
actual object AppBuildConfig {
    actual val VERSION_NAME: String
        get() = NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0.0"
}
