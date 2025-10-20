package com.klyx.extension.internal

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.klyx.core.ContextHolder
import com.klyx.terminal.localProcess
import com.klyx.terminal.ubuntuProcess
import com.klyx.terminal.userHomeDir
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

actual fun findBinary(binaryName: String): String? {
    with(ContextHolder.context) {
        val result = runCatching { ubuntuProcess("which", binaryName).executeBlocking() }.getOrNull()
        return result?.output?.ifEmpty { null }
    }
}

actual fun getenv(name: String): String? {
    with(ContextHolder.context) {
        val result = runCatching { ubuntuProcess("printenv", name).executeBlocking() }.getOrNull()
        return result?.output?.trim()?.ifEmpty { null }
    }
}

actual fun getenv(): Map<String, String> {
    with(ContextHolder.context) {
        val result = runCatching {
            ubuntuProcess("printenv").executeBlocking()
        }.getOrElse { return emptyMap() }

        if (!result.success) return emptyMap()

        return result.output
            .lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx == -1) null
                else line.substring(0, idx) to line.substring(idx + 1)
            }
            .toMap()
    }
}

actual fun executeCommand(
    command: String,
    args: Array<String>,
    env: Map<String, String>
): Output {
    with(ContextHolder.context) {
        val result = runCatching {
            localProcess(command, *args) { env(env) }.executeBlocking()
        }.getOrElse {
            return Output(
                status = -1,
                stdout = it.message ?: "Unknown error",
                stderr = it.message ?: "Unknown error"
            )
        }

        return Output(
            result.exitCode,
            result.output,
            result.error
        )
    }
}

actual val userHomeDir: String?
    get() = with(ContextHolder.context) { userHomeDir?.absolutePath }
