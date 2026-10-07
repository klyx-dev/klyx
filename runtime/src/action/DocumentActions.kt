package com.klyx.runtime.action

import com.klyx.core.action.Action
import com.klyx.core.action.ActionDescriptor
import com.klyx.core.action.ActionId
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.DocumentLocation
import okio.Path

/**
 * Opens a document into the active editor group, creating the first group if the workspace has none.
 *
 * A [preview] tab is temporary: the next preview tab opened in the same group replaces it. Opening the
 * same location twice returns the same document.
 */
data class OpenDocument(
    val location: DocumentLocation,
    val name: String,
    val preview: Boolean = false,
) : Action {
    override val id = OpenDocument.id

    companion object : ActionDescriptor<OpenDocument> {
        override val id = ActionId("editor.openDocument")
    }
}

/**
 * Opens a document into the active editor group, creating the first group if the workspace has none.
 *
 * A [preview] tab is temporary: the next preview tab opened in the same group replaces it. Opening the
 * same location twice returns the same document.
 */
fun OpenDocument(path: Path, preview: Boolean = false): OpenDocument =
    OpenDocument(
        location = DocumentLocation.Local(path),
        name = path.name,
        preview = preview
    )

/**
 * Closes a tab, in whichever editor group holds it. The workspace picks a neighbouring tab to focus.
 */
data class CloseDocument(val documentId: DocumentId) : Action {
    override val id = CloseDocument.id

    companion object : ActionDescriptor<CloseDocument> {
        override val id = ActionId("editor.closeDocument")
    }
}

/** Focuses a tab, in whichever editor group holds it. */
data class ActivateDocument(val documentId: DocumentId) : Action {
    override val id = ActivateDocument.id

    companion object : ActionDescriptor<ActivateDocument> {
        override val id = ActionId("editor.activateDocument")
    }
}

/** Pins a tab, so a later preview tab cannot replace it. Pinning also clears the preview flag. */
data class PinDocument(val documentId: DocumentId) : Action {
    override val id = PinDocument.id

    companion object : ActionDescriptor<PinDocument> {
        override val id = ActionId("editor.pinDocument")
    }
}

/** Unpins a tab, leaving it otherwise untouched. */
data class UnpinDocument(val documentId: DocumentId) : Action {
    override val id = UnpinDocument.id

    companion object : ActionDescriptor<UnpinDocument> {
        override val id = ActionId("editor.unpinDocument")
    }
}

/** Turns a preview tab into a permanent one, so opening another preview document keeps this tab. */
data class PromotePreviewDocument(val documentId: DocumentId) : Action {
    override val id = PromotePreviewDocument.id

    companion object : ActionDescriptor<PromotePreviewDocument> {
        override val id = ActionId("editor.promotePreviewDocument")
    }
}

/**
 * Closes the tab in focus.
 */
object CloseActiveDocument : Action {
    override val id = ActionId("editor.closeActiveDocument")
}

/** Focuses the tab to the right of the focused one, wrapping around. */
object ActivateNextDocument : Action {
    override val id = ActionId("editor.activateNextDocument")
}

/** Focuses the tab to the left of the focused one, wrapping around. */
object ActivatePreviousDocument : Action {
    override val id = ActionId("editor.activatePreviousDocument")
}
