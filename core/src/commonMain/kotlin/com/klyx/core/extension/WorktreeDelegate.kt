package com.klyx.core.extension

import kotlinx.io.files.Path

interface WorktreeDelegate {
    fun id(): ULong
    fun rootPath(): String

    suspend fun readTextFile(path: Path): Result<String>
    suspend fun which(binaryName: String): String?
    suspend fun shellEnv(): Map<String, String>
}
