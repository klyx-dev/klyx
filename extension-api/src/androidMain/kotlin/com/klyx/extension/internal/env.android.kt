package com.klyx.extension.internal

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.klyx.core.process.systemProcess
import com.klyx.core.terminal.sandboxDir
import com.klyx.core.terminal.userHomeDir
import com.klyx.core.withAndroidContext
import java.io.File

actual fun makeFileExecutable(path: String): Result<Unit, String> {
    val file = File(path)

    if (!file.exists()) {
        return Err("No such file: $path")
    }

    if (file.isDirectory) {
        return Err("Path points to a directory: $path")
    }

    return try {
        if (file.setExecutable(true)) {
            Ok(Unit)
        } else {
            Err("Permission change not applied (setExecutable returned false)")
        }
    } catch (e: SecurityException) {
        Err("Permission denied: ${e.message ?: "security restriction"}")
    } catch (e: Exception) {
        Err("Unexpected error: ${e.message ?: e::class.simpleName}")
    }
}

actual suspend fun getenv(name: String): String? = systemProcess(arrayOf("printenv", name))
    .output()
    .stdout
    .ifBlank { null }

actual suspend fun getenv(): Map<String, String> = systemProcess(arrayOf("printenv"))
    .output()
    .stdout
    .lineSequence()
    .mapNotNull { line ->
        val idx = line.indexOf('=')
        if (idx == -1) null
        else line.take(idx) to line.substring(idx + 1)
    }
    .toMap()

actual val userHomeDir: String?
    get() = withAndroidContext { userHomeDir?.absolutePath }

actual val rootDir: String
    get() = withAndroidContext { sandboxDir.absolutePath }
