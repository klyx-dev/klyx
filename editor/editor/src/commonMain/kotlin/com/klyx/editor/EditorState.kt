package com.klyx.editor

sealed interface EditorState {
    fun copy(): EditorState
}

@OptIn(ExperimentalCodeEditorApi::class)
class SoraEditorState(val state: CodeEditorState) : EditorState {
    override fun copy(): EditorState {
        return SoraEditorState(CodeEditorState(state))
    }
}

class ComposeEditorState(val state: com.klyx.editor.compose.CodeEditorState) : EditorState {
    override fun copy(): EditorState {
        return ComposeEditorState(state.copy())
    }
}
