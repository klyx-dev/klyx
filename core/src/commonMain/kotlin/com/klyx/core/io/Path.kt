package com.klyx.core.io

import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.file.toOkioPath
import kotlinx.serialization.Serializable
import okio.Path.Companion.toPath
import kotlin.jvm.JvmInline

@JvmInline
@Serializable
value class Path(
    @Serializable(with = PathSerializer::class)
    private val path: kotlinx.io.files.Path
) : Comparable<Path> {

    constructor(path: okio.Path) : this(path.toKotlinxIoPath())
    constructor(path: String, normalize: Boolean = true) : this(path.toPath(normalize = normalize).toKotlinxIoPath())

    val parent get() = path.parent?.let { Path(it) }

    val name get() = path.name

    @Suppress("NOTHING_TO_INLINE")
    inline fun fileName() = name

    override fun toString() = path.toString()

    override operator fun compareTo(other: Path): Int {
        return toOkioPath().compareTo(other.path.toOkioPath())
    }

    operator fun compareTo(other: okio.Path) = compareTo(Path(other))
    operator fun compareTo(other: kotlinx.io.files.Path) = compareTo(Path(other))

    fun toKotlinxIoPath() = path
    fun toOkioPath() = path.toOkioPath()

    fun join(vararg paths: String) = path.join(paths = paths).let(::Path)
}
