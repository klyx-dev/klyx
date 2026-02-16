package com.klyx.core.process

import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserName

actual suspend fun getenv(name: String): String? {
    val env = NSProcessInfo.processInfo.environment
    val value = env[name] as? String
    return value
}

actual suspend fun getenv(): Map<String, String> {
    val env = NSProcessInfo.processInfo.environment
    return buildMap {
        env.forEach { (key, value) ->
            if (value is String && key is String) {
                put(key, value)
            }
        }
    }
}

actual val systemUserName: String
    get() = NSUserName()

actual suspend fun systemEnv() = getenv()
