package com.bookd.app

import android.app.Application

class BookdApplication : Application() {

    companion object {
        lateinit var instance: BookdApplication
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}