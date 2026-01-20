package com.bookd.app.data.db

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import com.bookd.app.Database
import org.w3c.dom.Worker

actual class DatabaseDriverFactory {
    @Suppress("UnsafeCastFromDynamic")
    actual fun createDriver(): SqlDriver {
        val worker: Worker = js("""new Worker(new URL("./sqljs.worker.js", import.meta.url))""")
        return WebWorkerDriver(worker).also { 
            Database.Schema.synchronous().create(it) 
        }
    }
}
