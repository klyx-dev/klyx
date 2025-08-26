package com.klyx.extension.api

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.klyx.core.Environment
import com.klyx.extension.internal.getenv
import kotlinx.datetime.Clock
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

class Worktree(val id: Long, val rootPath: String) {
    private val fs = SystemFileSystem
    private val worktreePath = Path(rootPath)

    fun readTextFile(path: String): Result<String, String> {
        return try {
            val source = fs.source(Path(worktreePath, path)).buffered()
            Ok(source.peek().readString())
        } catch (err: Exception) {
            Err(err.message ?: "Failed to read text file: $path in worktree $id")
        }
    }

    fun which(binaryName: String): Option<String> {
        val paths = getenv("PATH")?.split(":").orEmpty()

        for (pathDir in paths) {
            val binaryPath = Path(pathDir, binaryName)
            val metadata = fs.metadataOrNull(binaryPath)

            if (metadata != null && !metadata.isDirectory) {
                return Some(binaryPath.toString())
            }
        }

        return None
    }

    fun shellEnv(): List<Pair<String, String>> {
        return getenv().map { (key, value) -> key to value }
    }
}

fun Worktree(path: String) = run {
    val id = Clock.System.now().toEpochMilliseconds()
    Worktree(id, path)
}

val SystemWorktree = Worktree(Environment.DeviceHomeDir)
