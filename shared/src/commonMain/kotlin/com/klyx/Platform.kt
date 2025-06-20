package com.klyx

interface Platform {
    val name: String
}

val Platform.isAndroid: Boolean
    get() = name.contains("android", ignoreCase = true)

expect fun getPlatform(): Platform
