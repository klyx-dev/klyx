package com.klyx.extension.internal

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
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
