package com.klyx.core.process

import io.itsvks.anyhow.anyhow
import io.itsvks.anyhow.map
import kotlinx.io.files.Path

expect suspend fun getenv(name: String): String?
expect suspend fun getenv(): Map<String, String>

expect val systemUserName: String

suspend fun which(binaryName: String) = anyhow {
    Command.newCommand("which")
        .arg(binaryName)
        .output()
        .map {
            if (it.stdout.isBlank()) bail($$"failed to find '$$binaryName' in $PATH")
            else Path(it.stdout)
        }
        .bind()
}
