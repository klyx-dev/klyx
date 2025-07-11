package com.klyx.editor

import androidx.compose.runtime.Stable
import com.klyx.editor.event.Event
import kotlin.reflect.KProperty

@Stable
@ExperimentalCodeEditorApi
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class CodeEditorState(
    initialText: String = ""
) {
    inline fun <reified E : Event> subscribeEvent(crossinline onEvent: (E) -> Unit)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String
    operator fun setValue(thisRef: Any?, property: KProperty<*>, text: String)
}

@ExperimentalCodeEditorApi
expect fun CodeEditorState(other: CodeEditorState): CodeEditorState
