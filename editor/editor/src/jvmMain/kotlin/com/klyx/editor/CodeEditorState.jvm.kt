package com.klyx.editor

import androidx.compose.runtime.Stable
import com.klyx.editor.event.Event
import kotlin.reflect.KProperty

@Stable
@ExperimentalCodeEditorApi
@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class CodeEditorState actual constructor(
    initialText: String
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
}

@ExperimentalCodeEditorApi
actual fun CodeEditorState(other: CodeEditorState): CodeEditorState {
    TODO("Not yet implemented")
}
