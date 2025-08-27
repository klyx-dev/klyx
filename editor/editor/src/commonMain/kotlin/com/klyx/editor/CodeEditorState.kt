package com.klyx.editor

import androidx.compose.runtime.Stable
import com.klyx.editor.event.Event
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KProperty

@Stable
@ExperimentalCodeEditorApi
expect class CodeEditorState(
    initialText: String = ""
) {
    val cursor: StateFlow<CursorState>

    inline fun <reified E : Event> subscribeEvent(crossinline onEvent: (E) -> Unit)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String
    operator fun setValue(thisRef: Any?, property: KProperty<*>, text: String)
}

@ExperimentalCodeEditorApi
expect fun CodeEditorState(other: CodeEditorState): CodeEditorState
