package com.klyx.core.file

import androidx.compose.runtime.Immutable
import com.klyx.core.extension.WorktreeDelegate
import com.klyx.core.io.join
import com.klyx.core.process.getenv
import io.itsvks.anyhow.anyhow
import io.itsvks.anyhow.context
import io.itsvks.anyhow.fold
import io.itsvks.anyhow.identity
import io.itsvks.anyhow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

typealias WorktreeId = Long

/**
 * A Klyx worktree.
 *
 * @property rootFile the root file of the worktree.
 */
@Immutable
@Serializable
data class Worktree(val rootFile: KxFile) : WorktreeDelegate {
    @Transient
    private val fs = SystemFileSystem

    @Transient
    private val worktreePath = Path(rootFile.absolutePath)

    val name get() = rootFile.name

    private val id: WorktreeId get() = hashCode().toLong()

    override fun id() = id

    override fun rootPath() = rootFile.absolutePath

    /**
     * Returns the textual contents of the specified file in the worktree.
     */
    override suspend fun readTextFile(path: Path) = anyhow {
        withContext(Dispatchers.IO) {
            val source = fs.source(worktreePath.join(path)).buffered()
            source.readString()
        }
    }.context("Failed to read text file: $path in worktree $this")

    /**
     * Returns the path to the given binary name, if one is present on the `$PATH`.
     */
    override suspend fun which(binaryName: String) = com.klyx.core.process.which(binaryName)
        .map { it.toString() }
        .fold(::identity) { null }

    /**
     * Returns the current shell environment.
     */
    override suspend fun shellEnv() = getenv()

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
