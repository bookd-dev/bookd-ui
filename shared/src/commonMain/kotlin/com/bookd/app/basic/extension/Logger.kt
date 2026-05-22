package com.bookd.app.basic.extension

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter
import co.touchlab.kermit.loggerConfigInit

/**
 * 全局日志记录器
 * 
 * 使用 Kermit 库提供跨平台日志功能
 */
object AppLogger : Logger(
    loggerConfigInit(
        platformLogWriter(),
        minSeverity = Severity.Debug
    ),
    tag = "Bookd"
)

/**
 * 调试日志
 * 
 * @param tag 日志标签，默认为 "Bookd"
 * @param message 日志消息，使用 lambda 懒求值
 */
fun logD(tag: String = "Bookd", message: () -> String) {
    AppLogger.d(tag = tag) { message() }
}

/**
 * 信息日志
 * 
 * @param tag 日志标签，默认为 "Bookd"
 * @param message 日志消息，使用 lambda 懒求值
 */
fun logI(tag: String = "Bookd", message: () -> String) {
    AppLogger.i(tag = tag) { message() }
}

/**
 * 警告日志
 * 
 * @param tag 日志标签，默认为 "Bookd"
 * @param message 日志消息，使用 lambda 懒求值
 */
fun logW(tag: String = "Bookd", message: () -> String) {
    AppLogger.w(tag = tag) { message() }
}

/**
 * 错误日志
 * 
 * @param tag 日志标签，默认为 "Bookd"
 * @param message 日志消息，使用 lambda 懒求值
 */
fun logE(tag: String = "Bookd", message: () -> String) {
    AppLogger.e(tag = tag) { message() }
}
