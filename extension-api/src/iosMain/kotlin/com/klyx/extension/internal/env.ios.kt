package com.klyx.extension.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.Foundation.NSProcessInfo

actual fun getenv(): Map<String, String> {
    return NSProcessInfo.processInfo.environment
        .mapKeys { (key, _) -> key.toString() }
        .mapValues { (_, value) -> value.toString() }
}

@OptIn(ExperimentalForeignApi::class)
actual fun getenv(name: String) = platform.posix.getenv(name)?.toKString()
