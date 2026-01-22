package com.bookd.app

/**
 * Android 平台构建配置
 */
actual object AppBuildConfig {
    actual val VERSION_NAME: String
        get() = try {
            val context = BookdApplication.instance
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
}
