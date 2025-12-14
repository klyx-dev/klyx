package com.klyx.core.extension

import io.itsvks.anyhow.AnyhowResult
import kotlinx.io.files.Path

interface WorktreeDelegate {
    fun id(): Long
    fun rootPath(): String

    suspend fun readTextFile(path: Path): AnyhowResult<String>
    suspend fun which(binaryName: String): String?
    suspend fun shellEnv(): Map<String, String>
}
