package com.klyx.runtime.action

import com.klyx.core.action.Action
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.DocumentLocation
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.Serializable
import okio.Path

/**
 * Opens a document into the active editor group, creating the first group if the workspace has none.
 *
 * A [preview] tab is temporary: the next preview tab opened in the same group replaces it. Opening the
 * same location twice returns the same document.
 */
@Serializable
data class OpenDocument(
    val location: DocumentLocation,
    val name: String,
    val preview: Boolean = false,
) : Action

/**
 * Opens a document into the active editor group, creating the first group if the workspace has none.
 *
 * A [preview] tab is temporary: the next preview tab opened in the same group replaces it. Opening the
 * same location twice returns the same document.
 */
fun OpenDocument(path: Path, preview: Boolean = false): OpenDocument =
    OpenDocument(
        location = DocumentLocation.Platform(PlatformFile(path.toString())),
        name = path.name,
        preview = preview
    )

/**
 * Closes a tab, in whichever editor group holds it. The workspace picks a neighbouring tab to focus.
 */
@Serializable
data class CloseDocument(val documentId: DocumentId) : Action

/** Focuses a tab, in whichever editor group holds it. */
@Serializable
data class ActivateDocument(val documentId: DocumentId) : Action

/** Pins a tab, so a later preview tab cannot replace it. Pinning also clears the preview flag. */
@Serializable
data class PinDocument(val documentId: DocumentId) : Action

/** Unpins a tab, leaving it otherwise untouched. */
@Serializable
data class UnpinDocument(val documentId: DocumentId) : Action

/** Turns a preview tab into a permanent one, so opening another preview document keeps this tab. */
@Serializable
data class PromotePreviewDocument(val documentId: DocumentId) : Action

/**
 * Closes the tab in focus.
 */
@Serializable
object CloseActiveDocument : Action

/** Focuses the tab to the right of the focused one, wrapping around. */
@Serializable
object ActivateNextDocument : Action

/** Focuses the tab to the left of the focused one, wrapping around. */
@Serializable
object ActivatePreviousDocument : Action

/** Saves a document to disk. */
@Serializable
data class SaveDocument(val documentId: DocumentId) : Action

/** Saves the document currently in focus. */
@Serializable
object SaveActiveDocument : Action

/** Saves all open documents to disk. */
@Serializable
object SaveAllDocuments : Action
