package com.klyx.data.fs

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

/**
 * Symbolic-link-safe file operations. None of these functions ever follows a
 * symlink while deleting, copying, or moving: links are unlinked or re-created
 * as links, and their targets are never traversed.
 */
object SafeFileOps {

    /** Deletes [target] (file, directory, or symlink) without following symlinks. */
    fun delete(target: Path) {
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return

        Files.walkFileTree(
            target,
            emptySet(),
            Int.MAX_VALUE,
            object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    // Symlinks surface here too; Files.deleteIfExists unlinks them.
                    Files.deleteIfExists(file)
                    return FileVisitResult.CONTINUE
                }

                override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                    exc?.let { throw it }
                    Files.deleteIfExists(dir)
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException) = throw exc
            }
        )
    }

    /**
     * Moves [source] into [targetDir] under [targetName] (defaults to its own name) —
     * atomically when possible, otherwise copy-then-delete across filesystems.
     * The source is removed only after the copy has fully succeeded, so a failure
     * never loses data.
     */
    fun moveInto(source: Path, targetDir: Path, targetName: String = source.fileName.toString()): Path {
        Files.createDirectories(targetDir)
        val target = targetDir.resolve(targetName)

        try {
            return Files.move(source, target)
        } catch (_: IOException) {
            // cross-device or locked rename: fall through to copy+delete.
        }

        copyInto(source, target)
        delete(source)
        return target
    }

    /** Copies [source] into [targetDir] without following symlinks. Returns the copy root. */
    fun copyTree(source: Path, targetDir: Path): Path {
        val target = targetDir.resolve(source.fileName)
        copyInto(source, target)
        return target
    }

    /** Copies [source] to the exact destination [target] without following symlinks. */
    private fun copyInto(source: Path, target: Path) {
        if (Files.isSymbolicLink(source)) {
            Files.createSymbolicLink(target, Files.readSymbolicLink(source))
            return
        }

        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            Files.copy(source, target)
            return
        }

        Files.createDirectories(target)
        Files.walkFileTree(
            source,
            emptySet(),
            Int.MAX_VALUE,
            object : SimpleFileVisitor<Path>() {
                fun destinationOf(path: Path): Path = target.resolve(source.relativize(path))

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    Files.createDirectories(destinationOf(dir))
                    return FileVisitResult.CONTINUE
                }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val destination = destinationOf(file)
                    if (attrs.isSymbolicLink) {
                        Files.createSymbolicLink(destination, Files.readSymbolicLink(file))
                    } else {
                        Files.copy(file, destination)
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                    throw exc
            }
        )
    }

    /** Best-effort recursive size of [path], not counting symlink targets; 0 when unknown. */
    fun sizeOf(path: Path): Long = try {
        var total = 0L
        if (!Files.isDirectory(path, LinkOption.NOFOLLOW_LINKS)) {
            total = Files.size(path)
        } else {
            Files.walkFileTree(
                path,
                emptySet(),
                Int.MAX_VALUE,
                object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (!attrs.isSymbolicLink) total += attrs.size()
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                        FileVisitResult.SKIP_SUBTREE
                }
            )
        }
        total
    } catch (_: Exception) {
        0L
    }
}
