package com.klyx.extension.internal

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
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

actual fun makeFileExecutable(path: String): Result<Unit, String> {
    val res = chmod(path, S_IXUSR.toUShort())
    return if (res == 0) {
        Ok(Unit)
    } else {
        Err("Failed to make file executable: $path")
    }
}

actual val userHomeDir: String?
    get() = TODO("Not yet implemented")

actual val rootDir: String
    get() = TODO("Not yet implemented")
