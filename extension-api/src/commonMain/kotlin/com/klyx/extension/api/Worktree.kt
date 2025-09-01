package com.klyx.extension.api

import arrow.core.Option
import arrow.core.toOption
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.klyx.core.Environment
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.extension.internal.findBinary
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
 * @property rootFile the root file of the worktree.
 */
class Worktree(val id: ULong, val rootFile: KxFile) {
    private val fs = SystemFileSystem
    private val worktreePath = Path(rootFile.absolutePath)

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
        val path = findBinary(binaryName)
        return path.toOption()
    }

    /**
     * Returns the current shell environment.
     */
    fun shellEnv(): List<Pair<String, String>> {
        return getenv().map { (key, value) -> key to value }
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + rootFile.absolutePath.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Worktree

        if (id != other.id) return false
        if (rootFile.absolutePath != other.rootFile.absolutePath) return false

        return true
    }
}

fun Worktree(path: String) = Worktree(path.toKxFile())
fun Worktree(file: KxFile) = WorktreeRegistry.register(file)

fun KxFile.toWorktree() = Worktree(this)

val SystemWorktree = Worktree(Environment.DeviceHomeDir)

private object WorktreeRegistry {
    private val map = mutableMapOf<ULong, KxFile>()
    private val counter = atomic(0L)

    fun register(worktreeRoot: KxFile): Worktree {
        val id = counter.getAndIncrement().toULong()
        val worktree = Worktree(id, worktreeRoot)
        map[id] = worktreeRoot
        return worktree
    }

    operator fun get(id: ULong) = map[id]?.let(::Worktree)

    fun unregister(id: ULong) = map.remove(id)?.let(::Worktree)
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
