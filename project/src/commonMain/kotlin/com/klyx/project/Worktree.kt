package com.klyx.project

import androidx.compose.runtime.Immutable
import arrow.core.raise.result
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.core.process.getenv
import com.klyx.core.util.join
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
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
@Immutable
@Serializable
data class Worktree(val rootFile: KxFile) {
    @Transient
    private val fs = SystemFileSystem

    @Transient
    private val worktreePath = Path(rootFile.absolutePath)

    val name get() = rootFile.name

    private val id get() = hashCode().toULong()

    fun id() = id

    fun rootPath() = rootFile.absolutePath

    /**
     * Returns the textual contents of the specified file in the worktree.
     */
    suspend fun readTextFile(path: Path) = result {
        withContext(Dispatchers.IO) {
            val source = fs.source(worktreePath.join(path)).buffered()
            source.readString()
        }
    }

    /**
     * Returns the path to the given binary name, if one is present on the `$PATH`.
     */
    suspend fun which(binaryName: String) = com.klyx.core.process.which(binaryName)
        .map { it.toString() }
        .getOrNull()

    /**
     * Returns the current shell environment.
     */
    suspend fun shellEnv() = getenv()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Worktree

        return rootFile.path == other.rootFile.path
    }

    override fun hashCode() = rootFile.path.hashCode()
}

fun Worktree(path: String) = Worktree(path.toKxFile())

fun KxFile.toWorktree() = Worktree(this)
fun KxFile.parentAsWorktree() = parentFile?.toWorktree()
fun KxFile.parentAsWorktreeOrSelf() = parentAsWorktree() ?: toWorktree()
