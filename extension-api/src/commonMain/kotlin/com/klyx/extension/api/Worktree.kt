package com.klyx.extension.api

import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.klyx.core.Environment
import com.klyx.core.file.KxFile
import com.klyx.extension.internal.getenv
import kotlinx.atomicfu.atomic
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

/**
 * A Klyx worktree.
 *
 * @property id the ID of the worktree.
 * @property rootPath the root path of the worktree.
 */
class Worktree(val id: ULong, val rootPath: String) {
    private val fs = SystemFileSystem
    private val worktreePath = Path(rootPath)

    /**
     * Returns the textual contents of the specified file in the worktree.
     */
    fun readTextFile(path: String): Result<String, String> {
        return try {
            val source = fs.source(Path(worktreePath, path)).buffered()
            Ok(source.peek().readString())
        } catch (err: Exception) {
            Err(err.message ?: "Failed to read text file: $path in worktree $id")
        }
    }

    /**
     * Returns the path to the given binary name, if one is present on the `$PATH`.
     */
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

    /**
     * Returns the current shell environment.
     */
    fun shellEnv(): List<Pair<String, String>> {
        return getenv().map { (key, value) -> key to value }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + rootPath.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Worktree

        if (id != other.id) return false
        if (rootPath != other.rootPath) return false

        return true
    }
}

fun Worktree(path: String) = WorktreeRegistry.register(path)
fun Worktree(file: KxFile) = Worktree(file.absolutePath)

val SystemWorktree = Worktree(Environment.DeviceHomeDir)

private object WorktreeRegistry {
    private val map = mutableMapOf<ULong, Worktree>()
    private val counter = atomic(0L)

    fun register(path: String): Worktree {
        val id = counter.getAndIncrement().toULong()
        val worktree = Worktree(id, path)
        map[id] = worktree
        return worktree
    }

    operator fun get(id: ULong) = map[id]

    fun unregister(id: ULong) = map.remove(id)
}

/**
 * A Klyx project.
 *
 * @property worktreeIds the IDs of all of the worktrees in this project.
 */
data class Project(val worktreeIds: List<ULong>) {
    fun hasWorktree(id: ULong) = worktreeIds.contains(id)
    fun isNotEmpty() = worktreeIds.isNotEmpty()
    fun isEmpty() = worktreeIds.isEmpty()

    val worktrees: List<Worktree>
        get() = worktreeIds.mapNotNull { WorktreeRegistry[it] }
}
