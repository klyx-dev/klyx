package com.klyx.data.editor

import io.github.rosemoe.sora.compose.CodeEditorState
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap

@Single
class EditorStateRegistry {
    private val states = ConcurrentHashMap<String, CodeEditorState>()
    private val baselineTexts = ConcurrentHashMap<String, String>()

    operator fun get(tabId: String) = states[tabId]
    operator fun set(tabId: String, state: CodeEditorState) = register(tabId, state)

    fun register(tabId: String, state: CodeEditorState) {
        states[tabId] = state
    }

    fun unregister(tabId: String) {
        states.remove(tabId)
        baselineTexts.remove(tabId)
    }

    fun setBaselineText(tabId: String, text: String) {
        baselineTexts[tabId] = text
    }

    fun getBaselineText(tabId: String): String? = baselineTexts[tabId]

    operator fun contains(tabId: String) = states.containsKey(tabId)

    fun clear() {
        states.clear()
        baselineTexts.clear()
    }

    val size: Int get() = states.size
}
