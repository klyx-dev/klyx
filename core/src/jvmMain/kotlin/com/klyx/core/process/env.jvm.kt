package com.klyx.core.process

actual suspend fun getenv(name: String): String? {
    return System.getenv(name)
}

actual suspend fun getenv(): Map<String, String> {
    return System.getenv().orEmpty()
}

actual val systemUserName: String
    get() = System.getProperty("user.name") ?: error("Unable to resolve the current system user")

actual suspend fun systemEnv() = System.getenv().orEmpty()
