package com.klyx.extension.internal

import com.klyx.core.process.systemProcess
import com.klyx.core.terminal.sandboxDir
import com.klyx.core.terminal.userHomeDir
import com.klyx.core.withAndroidContext
import io.itsvks.anyhow.anyhow
import java.io.File

actual fun makeFileExecutable(path: String) = anyhow {
    val file = File(path)

    ensure(file.exists()) { "No such file: $path" }
    ensure(!file.isDirectory) { "Path points to a directory: $path" }

    try {
        if (!file.setExecutable(true)) {
            bail("Permission change not applied (setExecutable returned false)")
        }
    } catch (e: SecurityException) {
        bail("Permission denied: ${e.message ?: "security restriction"}")
    } catch (e: Exception) {
        bail("Unexpected error: ${e.message ?: e::class.simpleName}")
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
