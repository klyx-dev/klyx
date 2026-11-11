package com.klyx.runtime.action

import com.klyx.core.action.Action
import com.klyx.runtime.workspace.EditorGroupId
import kotlinx.serialization.Serializable

/** Adds an editor group to the active workspace and focuses it. */
@Serializable
object SplitEditor : Action

@Serializable
object Undo : Action

@Serializable
object Redo : Action

@Serializable
data class InsertText(val text: String) : Action

/** Closes an editor group. The workspace keeps at least one group, so the last one is never closed. */
@Serializable
data class CloseEditorGroup(val groupId: EditorGroupId) : Action

/** Focuses an editor group, so keys and commands land in that split. */
@Serializable
data class ActivateEditorGroup(val groupId: EditorGroupId) : Action

/** Selects all text in the active editor. */
@Serializable
object SelectAll : Action

/** Copies selected text to clipboard. */
@Serializable
object Copy : Action

/** Cuts selected text to clipboard. */
@Serializable
object Cut : Action

/** Pastes text from clipboard into active editor. */
@Serializable
object Paste : Action

/** Folds all foldable regions in the active editor. */
@Serializable
object FoldAll : Action

/** Unfolds all folded regions in the active editor. */
@Serializable
object UnfoldAll : Action

/** Toggles folding at the specified line. */
@Serializable
data class ToggleFold(val line: Int) : Action

/** Triggers code completion popup at current cursor position. */
@Serializable
object TriggerCompletion : Action

/** Inserts a snippet with tab stops into the active editor. */
@Serializable
data class InsertSnippet(val snippet: String) : Action

/** Jumps cursor to the specified line and column. */
@Serializable
data class GotoLine(val line: Int, val column: Int = 0) : Action

/** Moves to the next search match. */
@Serializable
object FindNext : Action

/** Moves to the previous search match. */
@Serializable
object FindPrevious : Action

/** Clears active search highlight. */
@Serializable
object ClearSearch : Action
