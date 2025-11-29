package com.klyx.core.process

import com.klyx.core.anyResult
import com.klyx.core.bail
import kotlinx.io.files.Path

expect suspend fun getenv(name: String): String?
expect suspend fun getenv(): Map<String, String>

expect val systemUserName: String

suspend fun which(binaryName: String) = anyResult {
    systemProcess("which", binaryName)
        .output()
        .stdout
        .ifBlank { bail($$"failed to find '$$binaryName' in $PATH") }
        .let(::Path)
}
