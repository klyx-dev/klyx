package com.klyx.core.process

import arrow.core.raise.result
import kotlinx.io.files.Path

expect suspend fun getenv(name: String): String?
expect suspend fun getenv(): Map<String, String>

expect val systemUserName: String

suspend fun which(binaryName: String) = result {
    Command.newCommand("which")
        .arg(binaryName)
        .output()
        .map {
            if (it.stdout.isBlank()) raise(RuntimeException($$"failed to find '$$binaryName' in $PATH"))
            else Path(it.stdout)
        }
        .bind()
}
