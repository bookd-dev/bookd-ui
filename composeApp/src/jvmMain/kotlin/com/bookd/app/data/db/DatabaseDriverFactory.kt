package com.bookd.app.data.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bookd.app.Database
import java.io.File

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        // 在用户目录下创建数据库文件
        val databasePath = File(System.getProperty("user.home"), ".bookd")
        databasePath.mkdirs()
        val databaseFile = File(databasePath, DATABASE_NAME)
        
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
        Database.Schema.synchronous().create(driver)
        return driver
    }
}
