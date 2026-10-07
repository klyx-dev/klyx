package com.klyx.runtime.action

import com.klyx.core.action.Action
import com.klyx.core.action.ActionDescriptor
import com.klyx.core.action.ActionId
import com.klyx.runtime.workspace.EditorGroupId

/** Adds an editor group to the active workspace and focuses it. */
object SplitEditor : Action {
    override val id = ActionId("editor.splitEditor")
}

object Undo : Action {
    override val id = ActionId("editor.undo")
}

object Redo : Action {
    override val id = ActionId("editor.redo")
}

data class InsertText(val text: String) : Action {
    override val id = InsertText.id

    companion object : ActionDescriptor<InsertText> {
        override val id = ActionId("editor.insertText")
    }
}

/** Closes an editor group. The workspace keeps at least one group, so the last one is never closed. */
data class CloseEditorGroup(val groupId: EditorGroupId) : Action {
    override val id = CloseEditorGroup.id

    companion object : ActionDescriptor<CloseEditorGroup> {
        override val id = ActionId("editor.closeEditorGroup")
    }
}

/** Focuses an editor group, so keys and commands land in that split. */
data class ActivateEditorGroup(val groupId: EditorGroupId) : Action {
    override val id = ActivateEditorGroup.id

    companion object : ActionDescriptor<ActivateEditorGroup> {
        override val id = ActionId("editor.activateEditorGroup")
    }
}
