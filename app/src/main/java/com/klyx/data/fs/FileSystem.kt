package com.klyx.data.fs

import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.ContentResolver.SCHEME_CONTENT
import android.content.ContentResolver.SCHEME_FILE
import android.net.Uri
import android.provider.DocumentsContract
import com.klyx.data.file.KxFile
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.OutputStream

interface FileSystem {

    /**
     * Returns a list of files contained in the directory represented by this [uri].
     */
    fun list(uri: Uri): List<KxFile>

    /**
     * Open a stream on to the content associated with a content [Uri].  If there
     * is no data associated with the [Uri], [FileNotFoundException] is thrown.
     *
     * #### Accepts the following URI schemes:
     *
     * - content ([SCHEME_CONTENT])
     * - android.resource ([SCHEME_ANDROID_RESOURCE])
     * - file ([SCHEME_FILE])
     *
     * @param uri The desired [Uri].
     * @return [InputStream] or `null` if the provider recently crashed.
     * @throws FileNotFoundException if the provided URI could not be opened.
     */
    fun inputStream(uri: Uri): InputStream

    /**
     * Open a stream on to the content associated with a content [Uri].  If there
     * is no data associated with the [Uri], [FileNotFoundException] is thrown.
     *
     * #### Accepts the following URI schemes:
     *
     * - content ([SCHEME_CONTENT])
     * - file ([SCHEME_FILE])
     *
     * @param uri The desired [Uri].
     * @param mode The string representation of the file mode. Can be "r", "w", "wt", "wa", "rw"
     *             or "rwt". Please note the exact implementation of these may differ for each
     *             Provider implementation - for example, "w" may or may not truncate.
     * @return an [OutputStream] or `null` if the provider recently crashed.
     * @throws FileNotFoundException if the provided URI could not be opened.
     */
    fun outputStream(uri: Uri, mode: String = "w"): OutputStream

    /**
     * Delete the given document.
     *
     * @return `true` if the document was deleted successfully.
     * @throws FileNotFoundException
     */
    fun delete(uri: Uri): Boolean

    /**
     * Change the display name of an existing document.
     *
     * @return the existing or new document after the rename, or `null` if failed.
     * @throws FileNotFoundException
     */
    fun rename(uri: Uri, newName: String): Uri?

    /**
     * Create a new document with given MIME type and display name.
     *
     * @return newly created document, or `null` if failed
     * @throws FileNotFoundException
     */
    fun createFile(parent: Uri, name: String, mimeType: String): Uri?

    /**
     * Create a new directory with given display name.
     *
     * @return newly created directory, or `null` if failed
     * @throws FileNotFoundException
     */
    fun createDirectory(parent: Uri, name: String): Uri?

    /**
     * Returns a boolean indicating whether this file can be found.
     *
     * @return `true` if this file exists, `false` otherwise.
     */
    fun exists(uri: Uri): Boolean

    /**
     * Copies the given document.
     *
     * @return the copied document, or `null` if failed.
     * @param source document with [DocumentsContract.Document.FLAG_SUPPORTS_COPY]
     * @param targetParent document which will become a parent of the [source] document's copy.
     * @throws FileNotFoundException
     */
    fun copy(source: Uri, targetParent: Uri): Uri?

    /**
     * Moves the given document under a new parent.
     *
     * @param source document with [DocumentsContract.Document.FLAG_SUPPORTS_MOVE]
     * @param sourceParent parent document of the document to move.
     * @param targetParent document which will become a new parent of the [source] document.
     * @return the moved document, or `null` if failed.
     * @throws FileNotFoundException
     */
    fun move(source: Uri, sourceParent: Uri, targetParent: Uri): Uri?

    /**
     * Returns the [FileCapabilities] for the given [uri], indicating which operations
     * (like delete, rename, etc.) are supported by the underlying document provider.
     */
    fun capabilities(uri: Uri): FileCapabilities

    /**
     * Returns the display name of the document represented by the given [uri].
     *
     * @param uri The [Uri] of the file or directory.
     * @return The display name (typically the filename), or `null` if it could not be retrieved.
     */
    fun fileName(uri: Uri): String?
}
