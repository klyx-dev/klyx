package com.klyx.core.file

import arrow.core.None
import arrow.core.Some
import com.klyx.core.process.getenv
import io.itsvks.anyhow.anyhow
import io.itsvks.anyhow.context
import io.itsvks.anyhow.fold
import io.itsvks.anyhow.map
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * A Klyx worktree.
 *
 * @property rootFile the root file of the worktree.
 */
@Serializable
data class Worktree(val rootFile: KxFile) {
    @Transient
    private val fs = SystemFileSystem

    @Transient
    private val worktreePath = Path(rootFile.absolutePath)

    val name get() = rootFile.name

    val id get() = hashCode().toLong()

    /**
     * Returns the textual contents of the specified file in the worktree.
     */
    fun readTextFile(path: String) = anyhow {
        val source = fs.source(Path(worktreePath, path)).buffered()
        source.readString()
    }.context("Failed to read text file: $path in worktree $this")

    /**
     * Returns the path to the given binary name, if one is present on the `$PATH`.
     */
    suspend fun which(binaryName: String) = com.klyx.core.process.which(binaryName)
        .map { it.toString() }
        .fold(::Some) { None }

    /**
     * Returns the current shell environment.
     */
    suspend fun shellEnv(): List<Pair<String, String>> {
        return getenv().map { (key, value) -> key to value }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Worktree

        return rootFile.path == other.rootFile.path
    }

    override fun hashCode(): Int {
        return rootFile.path.hashCode()
    }
}

fun Worktree(path: String) = Worktree(path.toKxFile())

fun KxFile.toWorktree() = Worktree(this)
fun KxFile.parentAsWorktree() = parentFile?.toWorktree()
fun KxFile.parentAsWorktreeOrSelf() = parentAsWorktree() ?: toWorktree()
