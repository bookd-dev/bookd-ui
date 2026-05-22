package com.bookd.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform