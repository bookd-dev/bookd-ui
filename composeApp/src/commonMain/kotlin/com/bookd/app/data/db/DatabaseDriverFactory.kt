package com.bookd.app.data.db

import app.cash.sqldelight.db.SqlDriver

/**
 * 数据库驱动工厂
 * 
 * 使用 expect/actual 模式为各平台提供 SQLDelight 驱动
 */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

const val DATABASE_NAME = "bookd.db"
