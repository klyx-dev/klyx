package com.klyx.editor

import androidx.compose.runtime.Stable
import com.klyx.core.file.KxFile
import com.klyx.editor.event.Event
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KProperty

@Stable
@ExperimentalCodeEditorApi
expect class CodeEditorState(
    file: KxFile,
    project: KxFile? = file.parentFile
) {
    val cursor: StateFlow<CursorState>

    val file: KxFile
    val project: KxFile?

    inline fun <reified E : Event> subscribeEvent(crossinline onEvent: (E) -> Unit)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String
    operator fun setValue(thisRef: Any?, property: KProperty<*>, text: String)
}

@ExperimentalCodeEditorApi
expect fun CodeEditorState(other: CodeEditorState): CodeEditorState
