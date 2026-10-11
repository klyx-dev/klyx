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

/** Selects all text in the active editor. */
object SelectAll : Action {
    override val id = ActionId("editor.selectAll")
}

/** Copies selected text to clipboard. */
object Copy : Action {
    override val id = ActionId("editor.copy")
}

/** Cuts selected text to clipboard. */
object Cut : Action {
    override val id = ActionId("editor.cut")
}

/** Pastes text from clipboard into active editor. */
object Paste : Action {
    override val id = ActionId("editor.paste")
}

/** Folds all foldable regions in the active editor. */
object FoldAll : Action {
    override val id = ActionId("editor.foldAll")
}

/** Unfolds all folded regions in the active editor. */
object UnfoldAll : Action {
    override val id = ActionId("editor.unfoldAll")
}

/** Toggles folding at the specified line. */
data class ToggleFold(val line: Int) : Action {
    override val id = ToggleFold.id

    companion object : ActionDescriptor<ToggleFold> {
        override val id = ActionId("editor.toggleFold")
    }
}

/** Triggers code completion popup at current cursor position. */
object TriggerCompletion : Action {
    override val id = ActionId("editor.triggerCompletion")
}

/** Inserts a snippet with tab stops into the active editor. */
data class InsertSnippet(val snippet: String) : Action {
    override val id = InsertSnippet.id

    companion object : ActionDescriptor<InsertSnippet> {
        override val id = ActionId("editor.insertSnippet")
    }
}

/** Jumps cursor to the specified line and column. */
data class GotoLine(val line: Int, val column: Int = 0) : Action {
    override val id = GotoLine.id

    companion object : ActionDescriptor<GotoLine> {
        override val id = ActionId("editor.gotoLine")
    }
}

/** Moves to the next search match. */
object FindNext : Action {
    override val id = ActionId("editor.findNext")
}

/** Moves to the previous search match. */
object FindPrevious : Action {
    override val id = ActionId("editor.findPrevious")
}

/** Clears active search highlight. */
object ClearSearch : Action {
    override val id = ActionId("editor.clearSearch")
}
