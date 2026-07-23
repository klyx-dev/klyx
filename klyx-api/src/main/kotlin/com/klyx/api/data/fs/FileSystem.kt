package com.klyx.api.data.fs

import android.net.Uri
import com.klyx.api.data.file.FileStatInfo
import com.klyx.api.data.file.KxFile
import com.klyx.api.plugin.PluginService
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * Categorizes files based on their content type to determine how they should be handled in the UI.
 */
enum class FileCategory {

    /** A text-based file that can be opened in the code editor. */
    TEXT,

    /** An image file that can be displayed. */
    IMAGE,

    /** A binary file format that is currently not supported for viewing/editing. */
    BINARY_UNSUPPORTED,

    /** An error occurred while determining the file category. */
    ERROR
}

/**
 * A service for performing file system operations across both local storage and Android's
 * Storage Access Framework (SAF).
 *
 * ### Example
 * ```kotlin
 * val fs: FileSystem by plugin()
 *
 * // List files in a directory
 * val files = fs.list(directoryUri)
 *
 * // Read a file
 * fs.inputStream(fileUri).use { input ->
 *     val content = input.readBytes()
 * }
 * ```
 */
interface FileSystem : PluginService {

    /**
     * Lists the contents of the directory at the given [uri].
     *
     * @return A list of [KxFile] instances representing the directory's contents.
     */
    suspend fun list(uri: Uri): List<KxFile>

    /**
     * Opens an [InputStream] for reading the file at the given [uri].
     */
    suspend fun inputStream(uri: Uri): InputStream

    /**
     * Opens an [OutputStream] for writing to the file at the given [uri].
     *
     * @param mode The string representation of the file mode. Can be "r", "w", "wt", "wa", "rw" or "rwt".
     * Please note the exact implementation of these may differ for each Provider implementation -
     * for example, "w" may or may not truncate. Defaults to "w".
     */
    suspend fun outputStream(uri: Uri, mode: String = "w"): OutputStream

    /**
     * Deletes the file or directory at the given [uri].
     *
     * @return `true` if the deletion was successful.
     */
    suspend fun delete(uri: Uri): Boolean

    /**
     * Renames the file or directory at [uri] to [newName].
     *
     * @return The new [Uri] if successful, or null otherwise.
     */
    suspend fun rename(uri: Uri, newName: String): Uri?

    /**
     * Creates a new file in the specified [parent] directory.
     *
     * @param name The name of the file to create.
     * @param mimeType The MIME type of the file. Defaults to `* / *`.
     * @return The [Uri] of the newly created file, or null if creation failed.
     */
    suspend fun createFile(parent: Uri, name: String, mimeType: String = "*/*"): Uri?

    /**
     * Creates a new directory in the specified [parent] directory.
     *
     * @return The [Uri] of the newly created directory, or null if creation failed.
     */
    suspend fun createDirectory(parent: Uri, name: String): Uri?

    /**
     * Checks if a file or directory exists at the given [uri].
     */
    suspend fun exists(uri: Uri): Boolean

    /**
     * Copies the file or directory at [source] into [targetParent].
     *
     * @return The [Uri] of the copy if successful, or null otherwise.
     */
    suspend fun copy(source: Uri, targetParent: Uri): Uri?

    /**
     * Moves the file or directory at [source] from [sourceParent] to [targetParent].
     *
     * @return The [Uri] at the new location if successful, or null otherwise.
     */
    suspend fun move(source: Uri, sourceParent: Uri, targetParent: Uri): Uri?

    /**
     * Retrieves the [FileCapabilities] for the given [uri], describing what operations are allowed.
     */
    suspend fun capabilities(uri: Uri): FileCapabilities

    /**
     * Returns the display name of the file at the given [uri].
     */
    suspend fun fileName(uri: Uri): String?

    /**
     * Wraps a raw Android [Uri] into a [KxFile] instance.
     */
    suspend fun wrapUri(uri: Uri): KxFile

    /**
     * Determines the [FileCategory] for the given [uri] based on its MIME type or content.
     */
    suspend fun determineFileCategory(uri: Uri): FileCategory

    /**
     * Returns the MIME type of the file at [uri], or null if it cannot be determined.
     */
    suspend fun mimeType(uri: Uri): String?

    /**
     * Performs a recursive search for files matching the [query] string within the specified [roots].
     *
     * @param roots The list of directory URIs to search within.
     * @param query The search term (case-insensitive).
     * @param maxResults The maximum number of results to return. Defaults to 500.
     * @return A [Flow] emitting [KxFile] matches as they are found.
     */
    suspend fun search(roots: List<Uri>, query: String, maxResults: Int = 500): Flow<KxFile>

    /**
     * Returns detailed file statistics for the given [uri], or null if unavailable.
     */
    suspend fun stat(uri: Uri): FileStatInfo? = null

    /**
     * Returns a human-readable permission string (e.g. "rw-r--r--") for the given [uri].
     */
    suspend fun permissions(uri: Uri): String = "---------"

    /**
     * Returns whether the path at [uri] is a symbolic link.
     */
    suspend fun isSymlink(uri: Uri): Boolean = false

    /**
     * Returns the target of a symbolic link at [uri], or null if not a symlink or unsupported.
     */
    suspend fun symlinkTarget(uri: Uri): String? = null

    /**
     * Returns whether the path at [uri] is a protected system path that should not be modified.
     */
    suspend fun isProtectedPath(uri: Uri): Boolean = false

    /**
     * Returns a human-readable display name for the given [file].
     */
    suspend fun resolveName(file: KxFile): String = file.name

    /**
     * Calculates the total size of a directory tree at [uri].
     *
     * The default implementation uses [list] for recursive traversal. Override in each
     * filesystem for an optimized version using native APIs.
     */
    suspend fun calculateSize(uri: Uri): Flow<SizeProgress> = channelFlow {
        val file = wrapUri(uri)
        if (!file.isDirectory) {
            send(SizeProgress(file.size, fileCount = 1, dirCount = 0, isFinished = true))
            return@channelFlow
        }

        var totalSize = 0L
        var fileCount = 0
        var dirCount = 0

        val queue = ArrayDeque<Uri>()
        queue.add(uri)

        while (queue.isNotEmpty()) {
            if (!currentCoroutineContext().isActive) break
            val children = list(queue.removeFirst())
            for (child in children) {
                if (!currentCoroutineContext().isActive) break
                if (child.isDirectory) {
                    dirCount++
                    queue.add(child.uri)
                } else {
                    fileCount++
                    totalSize += child.size
                }
                send(SizeProgress(totalSize, fileCount, dirCount, isFinished = false))
            }
        }

        send(SizeProgress(totalSize, fileCount, dirCount, isFinished = true))
    }
}

/**
 * Convenience extension to create a directory and its parents if it doesn't exist.
 */
fun File.createDirIfMissing(): Boolean {
    return if (!exists()) mkdirs() else true
}
