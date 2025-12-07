package com.klyx.extension.internal

import io.itsvks.anyhow.anyhow
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.Foundation.NSProcessInfo
import platform.posix.S_IXUSR
import platform.posix.chmod

actual suspend fun getenv(): Map<String, String> {
    return NSProcessInfo.processInfo.environment
        .mapKeys { (key, _) -> key.toString() }
        .mapValues { (_, value) -> value.toString() }
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun getenv(name: String) = platform.posix.getenv(name)?.toKString()

actual fun makeFileExecutable(path: String) = anyhow {
    val res = chmod(path, S_IXUSR.toUShort())
    if (res != 0) {
        bail("Failed to make file executable: $path")
    }
}

actual val userHomeDir: String?
    get() = TODO("Not yet implemented")

actual val rootDir: String
    get() = TODO("Not yet implemented")
