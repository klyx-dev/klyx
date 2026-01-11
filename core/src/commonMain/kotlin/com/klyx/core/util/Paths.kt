package com.klyx.core.util

import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.file.toOkioPath
import com.klyx.core.io.okioFs
import com.klyx.core.platform.Os
import com.klyx.core.platform.currentOs
import kotlinx.io.files.Path

fun Path.join(vararg paths: String): Path {
    return Path(this, parts = paths)
}

fun Path.join(vararg paths: Path) = join(paths = paths.map(Path::toString).toTypedArray())

@Suppress("NOTHING_TO_INLINE")
inline fun emptyPath() = Path("")

fun String.intoPath() = Path(this)

fun Path.makeAbsolute(): Path {
    return if (isAbsolute) this else Path("/").join(this)
}

fun Path.isInside(other: Path): Boolean {
    return other.isAbsolute && other.toString().startsWith(this.toString())
}

fun okio.Path.isSymlink() = okioFs.metadataOrNull(this)?.symlinkTarget != null
fun Path.isSymlink() = this.toOkioPath().isSymlink()

fun Path.normalize() = this.toOkioPath().normalized().toKotlinxIoPath()

fun isAbsolute(path: String, style: PathStyle) =
    path.startsWith('/') || style == PathStyle.Windows && (path.startsWith('\\') || path.firstOrNull()
        ?.isLetter() == true && path.drop(1).removePrefix(":").let { it.startsWith('/') || it.startsWith('\\') })

enum class PathStyle {
    Posix, Windows;

    @Suppress("NOTHING_TO_INLINE")
    inline fun primarySeparator(): String = when (this) {
        Posix -> "/"
        Windows -> "\\"
    }

    fun separators() = when (this) {
        Posix -> listOf("/")
        Windows -> listOf("\\", "/")
    }

    fun separatorsChar() = when (this) {
        Posix -> listOf('/')
        Windows -> listOf('\\', '/')
    }

    fun isWindows() = this == Windows
    fun isPosix() = this == Posix

    companion object {
        fun local(): PathStyle = when (currentOs()) {
            Os.Windows -> Windows
            else -> Posix
        }
    }
}
