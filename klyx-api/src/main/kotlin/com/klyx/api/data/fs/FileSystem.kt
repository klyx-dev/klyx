package com.klyx.api.data.fs

import android.net.Uri
import com.klyx.api.data.file.KxFile
import com.klyx.api.plugin.PluginService
import kotlinx.coroutines.flow.Flow
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
}

/**
 * Convenience extension to create a directory and its parents if it doesn't exist.
 */
fun KxFile.createDirIfMissing(): Boolean {
    return if (!exists) mkdirs() else true
}

/**
 * Convenience extension to create a directory and its parents if it doesn't exist.
 */
fun File.createDirIfMissing(): Boolean {
    return if (!exists()) mkdirs() else true
}
