package com.klyx.editor.compose.selection

internal enum class Handle {
    Cursor,
    SelectionStart,
    SelectionEnd,
}

/**
 * The selection handle state of the TextField. It can be None, Selection or Cursor. It determines
 * whether the selection handle, cursor handle or only cursor is shown. And how TextField handles
 * gestures.
 */
internal enum class HandleState {
    /**
     * No selection is active in this TextField. This is the initial state of the TextField. If the
     * user long click on the text and start selection, the TextField will exit this state and
     * enters [HandleState.Selection] state. If the user tap on the text, the TextField will exit
     * this state and enters [HandleState.Cursor] state.
     */
    None,

    /**
     * Selection handle is displayed for this TextField. User can drag the selection handle to
     * change the selected text. If the user start editing the text, the TextField will exit this
     * state and enters [HandleState.None] state. If the user tap on the text, the TextField will
     * exit this state and enters [HandleState.Cursor] state.
     */
    Selection,

    /**
     * Cursor handle is displayed for this TextField. User can drag the cursor handle to change the
     * cursor position. If the user start editing the text, the TextField will exit this state and
     * enters [HandleState.None] state. If the user long click on the text and start selection, the
     * TextField will exit this state and enters [HandleState.Selection] state. Also notice that
     * TextField won't enter this state if the current input text is empty.
     */
    Cursor,
}
