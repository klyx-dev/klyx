package com.klyx.data.editor

import io.github.rosemoe.sora.compose.CodeEditorState
import org.koin.core.annotation.Single

@Single
class EditorStateRegistry {
    private val states = mutableMapOf<String, CodeEditorState>()

    operator fun get(tabId: String) = states[tabId]
    operator fun set(tabId: String, state: CodeEditorState) = register(tabId, state)

    fun register(tabId: String, state: CodeEditorState) {
        states[tabId] = state
    }

    fun unregister(tabId: String) {
        states.remove(tabId)
    }

    operator fun contains(tabId: String) = states.containsKey(tabId)

    fun clear() {
        states.clear()
    }
}
