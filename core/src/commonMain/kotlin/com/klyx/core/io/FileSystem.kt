package com.klyx.core.io

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.FileNotFoundException
import kotlinx.io.files.SystemFileSystem
import okio.SYSTEM

inline val fs get() = SystemFileSystem
inline val okioFs get() = okio.FileSystem.SYSTEM

interface FileSystem {
    /**
     * Returns `true` if there is a filesystem entity a [path] points to,
     * otherwise returns `false`.
     *
     * @param path the path that should be checked for existence.
     *
     * @throws kotlinx.io.IOException when the attempt to check the existence of the [path] failed.
     */
    suspend fun exists(path: Path): Boolean

    /**
     * Deletes a file or directory the [path] points to from a filesystem.
     * If there is no filesystem entity represented by the [path]
     * this method throws [FileNotFoundException] when [mustExist] is `true`.
     *
     * Note that in the case of a directory, this method will not attempt to delete it recursively,
     * so deletion of non-empty directory will fail.
     *
     * @param path the path to a file or directory to be deleted.
     * @param mustExist the flag indicating whether missing [path] is an error, `true` by default.
     *
     * @throws FileNotFoundException when [path] does not exist and [mustExist] is `true`.
     * @throws IOException if deletion failed.
     */
    suspend fun delete(path: Path, mustExist: Boolean = true)

    /**
     * Creates a directory tree represented by the [path].
     * If [path] already exists then the method throws [kotlinx.io.IOException] when [mustCreate] is `true`.
     * The call will attempt to create only missing directories.
     * The method is not atomic and if it fails after creating some
     * directories, these directories will not be deleted automatically.
     * Permissions for created directories are platform-specific.
     *
     * @param path the path to be created.
     * @param mustCreate the flag indicating that existence of [path] should be treated as an error,
     * by default it is `false`.
     *
     * @throws IOException when [path] already exists and [mustCreate] is `true`.
     * @throws IOException when the creation of one of the directories fails.
     * @throws IOException when [path] is an existing file and [mustCreate] is `false`.
     */
    suspend fun createDirectories(path: Path, mustCreate: Boolean = false)

    /**
     * Atomically renames [source] to [destination] overriding [destination] if it already exists.
     *
     * When the filesystem does not support atomic move of [source] and [destination] corresponds to different
     * filesystems (or different volumes, on Windows) and the operation could not be performed atomically,
     * [UnsupportedOperationException] is thrown.
     *
     * On some platforms, like Wasm-WASI, there is no way to tell if the underlying filesystem supports atomic move.
     * In such cases, the move will be performed and no [UnsupportedOperationException] will be thrown.
     *
     * When [destination] is an existing directory, the operation may fail on some platforms
     * (on Windows, particularly).
     *
     * @param source the path to rename.
     * @param destination desired path name.
     *
     * @throws FileNotFoundException when the [source] does not exist.
     * @throws IOException when the move failed.
     * @throws UnsupportedOperationException when the filesystem does not support atomic move.
     */
    suspend fun atomicMove(source: Path, destination: Path)

    /**
     * Returns [RawSource] to read from a file the [path] points to.
     *
     * How a source will read the data is implementation-specific and failures caused
     * by the missing file or, for example, lack of permissions may not be reported immediately,
     * but postponed until the source will try to fetch data.
     *
     * If [path] points to a directory, this method will fail with [IOException].
     *
     * @param path the path to read from.
     *
     * @throws FileNotFoundException when the file does not exist.
     * @throws IOException when it's not possible to open the file for reading.
     */
    suspend fun source(path: Path): RawSource

    /**
     * Returns [RawSink] to write into a file the [path] points to.
     * Depending on [append] value, the file will be overwritten or data will be appened to it.
     * File will be created if it does not exist yet.
     *
     * How a sink will write the data is implementation-specific and failures caused,
     * for example, by the lack of permissions may not be reported immediately,
     * but postponed until the sink will try to store data.
     *
     * If [path] points to a directory, this method will fail with [IOException]
     *
     * @param path the path to a file to write data to.
     * @param append the flag indicating whether the data should be appended to an existing file or it
     * should be overwritten, `false` by default, meaning the file will be overwritten.
     *
     * @throws IOException when it's not possible to open the file for writing.
     */
    suspend fun sink(path: Path, append: Boolean = false): RawSink

    /**
     * Return [FileMetadata] associated with a file or directory the [path] points to.
     * If there is no such file or directory, or it's impossible to fetch metadata,
     * `null` is returned.
     *
     * @param path the path to get the metadata for.
     */
    suspend fun metadataOrNull(path: Path): FileMetadata?

    /**
     * Returns an absolute path to the same file or directory the [path] is pointing to.
     * All symbolic links are solved, extra path separators and references to current (`.`) or
     * parent (`..`) directories are removed.
     * If the [path] is a relative path then it'll be resolved against current working directory.
     * If there is no file or directory to which the [path] is pointing to then [FileNotFoundException] will be thrown.
     *
     * @param path the path to resolve.
     * @return a resolved path.
     * @throws FileNotFoundException if there is no file or directory corresponding to the specified path.
     */
    suspend fun resolve(path: Path): Path

    /**
     * Returns paths corresponding to [directory]'s immediate children.
     *
     * There are no guarantees on children paths order within a returned collection.
     *
     * If path [directory] was an absolute path, a returned collection will also contain absolute paths.
     * If it was a relative path, a returned collection will contain relative paths.
     *
     * *For `wasmWasi` target, function does not work with NodeJS runtime on Windows,
     * as `fd_readdir` function is [not implemented there](https://github.com/nodejs/node/blob/6f4d6011ea1b448cf21f5d363c44e4a4c56ca34c/deps/uvwasi/src/uvwasi.c#L19).*
     *
     * @param directory a directory to list.
     * @return a collection of [directory]'s immediate children.
     * @throws FileNotFoundException if [directory] does not exist.
     * @throws IOException if [directory] points to something other than directory.
     * @throws IOException if there was an underlying error preventing listing [directory] children.
     */
    suspend fun list(directory: Path): Collection<Path>

    suspend fun isFile(path: Path) = metadataOrNull(path)?.isRegularFile ?: false
    suspend fun isDirectory(path: Path) = metadataOrNull(path)?.isDirectory ?: false
}

private class SystemFileSystemImpl(val delegate: kotlinx.io.files.FileSystem) : FileSystem {
    private suspend inline fun <R> withIO(
        noinline block: suspend CoroutineScope.() -> R
    ) = withContext(Dispatchers.IO, block)

    override suspend fun exists(path: Path) = withIO { delegate.exists(path.toKotlinxIoPath()) }
    override suspend fun delete(path: Path, mustExist: Boolean) =
        withIO { delegate.delete(path.toKotlinxIoPath(), mustExist) }

    override suspend fun createDirectories(path: Path, mustCreate: Boolean) =
        withIO { delegate.createDirectories(path.toKotlinxIoPath(), mustCreate) }

    override suspend fun atomicMove(source: Path, destination: Path) {
        withIO { delegate.atomicMove(source.toKotlinxIoPath(), destination.toKotlinxIoPath()) }
    }

    override suspend fun source(path: Path) = withIO { delegate.source(path.toKotlinxIoPath()) }

    override suspend fun sink(path: Path, append: Boolean) =
        withIO { delegate.sink(path.toKotlinxIoPath(), append) }

    override suspend fun metadataOrNull(path: Path): FileMetadata? {
        return delegate.metadataOrNull(path.toKotlinxIoPath())
    }

    override suspend fun resolve(path: Path) = withIO { Path(delegate.resolve(path.toKotlinxIoPath()).toString()) }

    override suspend fun list(directory: Path): Collection<Path> =
        withIO { delegate.list(directory.toKotlinxIoPath()).map { Path(it.toString()) } }
}

typealias Fs = FileSystem

val SystemFileSystem: FileSystem get() = SystemFileSystemImpl(SystemFileSystem)
