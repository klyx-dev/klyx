package com.klyx.extension.internal

import com.github.michaelbull.result.Result
import com.klyx.core.process.systemProcess

expect suspend fun getenv(name: String): String?
expect suspend fun getenv(): Map<String, String>

expect fun makeFileExecutable(path: String): Result<Unit, String>

suspend fun findBinary(binaryName: String): String? {
    return systemProcess(arrayOf("which", binaryName))
        .output()
        .stdout
        .ifBlank { null }
}

expect val userHomeDir: String?
expect val rootDir: String
