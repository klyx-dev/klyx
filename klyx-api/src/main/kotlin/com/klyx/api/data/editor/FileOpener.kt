package com.klyx.api.data.editor

import android.net.Uri
import com.klyx.api.plugin.KlyxPlugin
import com.klyx.api.plugin.PluginService

/**
 * Describes a file the user is attempting to open.
 *
 * A [FileOpenRequest] is handed to every registered [FileOpener] so a plugin can decide
 * whether it wants to handle the file; for example, opening a media player tab for an
 * audio or video file that Klyx cannot display natively.
 *
 * @property uri The URI of the file being opened.
 * @property fileName The display name of the file, including its extension.
 * @property extension The lowercase file extension without the leading dot, or an empty string if none.
 * @property mimeType The resolved MIME type, or null if it could not be determined.
 * @property projectUri The URI of the project the file belongs to, if any.
 */
data class FileOpenRequest(
    val uri: Uri,
    val fileName: String,
    val extension: String,
    val mimeType: String?,
    val projectUri: Uri? = null
)

/**
 * A handle returned when a [FileOpener] is registered.
 */
interface FileOpenerRegistration {

    /**
     * Removes the opener from the registry.
     */
    fun unregister()
}

/**
 * Handles opening files that Klyx does not support natively.
 *
 * When the user opens a file the app cannot display (an unsupported binary), every
 * registered [FileOpener] is consulted in descending [priority] order. The first opener
 * that returns a non-null [WorkspaceTab] from [open] wins and its tab is shown; if no
 * opener claims the file, the default "unsupported file" behaviour is used.
 *
 * ### Example
 * ```kotlin
 * class MediaOpener : FileOpener {
 *     override val id = "com.example.media.opener"
 *     override val priority = 10
 *
 *     override suspend fun open(request: FileOpenRequest): WorkspaceTab? {
 *         if (request.extension !in setOf("mp3", "mp4", "wav", "mkv")) return null
 *         return WorkspaceTab.Custom(
 *             title = request.fileName,
 *             id = request.uri.toString(),
 *         ) { MediaPlayer(request.uri) }
 *     }
 * }
 * ```
 */
interface FileOpener {

    /**
     * A unique identifier for this opener.
     *
     * Recommended format: reverse-DNS (e.g. "com.example.plugin.opener").
     */
    val id: String

    /**
     * Openers with a higher priority are consulted first. Defaults to 0.
     */
    val priority: Int get() = 0

    /**
     * Returns a [WorkspaceTab] to display for [request], or null to decline the file and
     * let other openers (or the default handling) take over.
     */
    suspend fun open(request: FileOpenRequest): WorkspaceTab?
}

/**
 * Manages the registration and lookup of [FileOpener]s.
 *
 * Resolve it from a plugin with `val openers: FileOpenerRegistry by plugin()` or
 * `context.service<FileOpenerRegistry>()`.
 *
 * ### Example
 * ```kotlin
 * val openers: FileOpenerRegistry by plugin()
 * private var registration: FileOpenerRegistration? = null
 *
 * override suspend fun onStart() {
 *     registration = openers.register(MediaOpener())
 * }
 *
 * override suspend fun onStop() {
 *     registration?.unregister()
 * }
 * ```
 */
interface FileOpenerRegistry : PluginService {

    /**
     * Registers an [opener]. Remember to [FileOpenerRegistration.unregister] it when the
     * plugin stops, or call [unregister] with its id. Registering an opener whose [FileOpener.id]
     * matches an existing one replaces the previous registration.
     */
    context(plugin: KlyxPlugin)
    fun register(opener: FileOpener): FileOpenerRegistration

    /**
     * Removes a previously registered opener by its [id].
     */
    fun unregister(id: String)

    /**
     * All registered openers, ordered by descending [FileOpener.priority].
     */
    fun openers(): List<FileOpener>

    /**
     * Consults every registered opener in priority order and returns the first non-null
     * [WorkspaceTab], or null if no opener claimed the [request].
     */
    suspend fun open(request: FileOpenRequest): WorkspaceTab?
}
