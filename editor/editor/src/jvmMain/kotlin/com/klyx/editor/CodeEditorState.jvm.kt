package com.klyx.editor

import androidx.compose.runtime.Stable
import com.klyx.core.file.KxFile
import com.klyx.editor.event.Event
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KProperty

@Stable
@ExperimentalCodeEditorApi
actual class CodeEditorState actual constructor(
    actual val file: KxFile,
    actual val project: KxFile?
) {
    actual operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): String {
        TODO("Not yet implemented")
    }

    actual operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        text: String
    ) {
    }

    actual inline fun <reified E : Event> subscribeEvent(crossinline onEvent: (E) -> Unit) {
    }

    actual val cursor: StateFlow<CursorState>
        get() = TODO("Not yet implemented")
}

@ExperimentalCodeEditorApi
actual fun CodeEditorState(other: CodeEditorState): CodeEditorState {
    TODO("Not yet implemented")
}
