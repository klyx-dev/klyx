package com.klyx.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.klyx.editor.event.ContentChangeEvent
import io.github.rosemoe.sora.event.Event
import io.github.rosemoe.sora.event.EventManager.NoUnsubscribeReceiver
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KProperty
import com.klyx.editor.event.Event as KlyxEvent

@Stable
@ExperimentalCodeEditorApi
@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual class CodeEditorState actual constructor(
    initialText: String,
) {
    @PublishedApi
    internal val pendingSubscriptions = mutableListOf<() -> Unit>()

    var editor: CodeEditor? = null
        set(value) {
            field = value
            if (value != null) {
                pendingSubscriptions.forEach { it() }
                pendingSubscriptions.clear()
            }
        }

    var content by mutableStateOf(Content(initialText))

    inline fun <reified T : Event> subscribeAlways(
        receiver: NoUnsubscribeReceiver<T>
    ): SubscriptionReceipt<T>? {
        val currentEditor = editor
        return if (currentEditor != null) {
            currentEditor.subscribeAlways(receiver)
        } else {
            pendingSubscriptions.add { editor?.subscribeAlways(receiver) }
            null
        }
    }

    fun setLanguage(language: Language) {
        editor?.setEditorLanguage(language)
    }

    @PublishedApi
    internal fun editorNotInitialized(): Nothing {
        error("Editor not initialized")
    }

    private val _cursor = MutableStateFlow(CursorState())
    actual val cursor = _cursor.asStateFlow()

    init {
        subscribeAlways<SelectionChangeEvent> { event ->
            val cursor = event.editor.cursor
            cursor.isSelected
            _cursor.update {
                CursorState(
                    left = cursor.left,
                    right = cursor.right,
                    rightLine = cursor.rightLine,
                    leftLine = cursor.leftLine,
                    rightColumn = cursor.rightColumn,
                    leftColumn = cursor.leftColumn,
                    isSelected = cursor.isSelected
                )
            }
        }
    }

    actual operator fun getValue(
        thisRef: Any?,
        property: KProperty<*>
    ): String = content.toString()

    actual operator fun setValue(
        thisRef: Any?,
        property: KProperty<*>,
        text: String
    ) {
        editor?.setText(text)
    }

    actual inline fun <reified E : KlyxEvent> subscribeEvent(crossinline onEvent: (E) -> Unit) {
        when (E::class) {
            ContentChangeEvent::class -> subscribeAlways<io.github.rosemoe.sora.event.ContentChangeEvent> {
                onEvent(ContentChangeEvent(it.changedText) as E)
            }
        }
    }
}

@ExperimentalCodeEditorApi
@Composable
fun rememberCodeEditorState(
    initialContent: Content = Content()
) = remember {
    CodeEditorState(initialContent.toString())
}

@ExperimentalCodeEditorApi
actual fun CodeEditorState(other: CodeEditorState): CodeEditorState {
    return CodeEditorState(initialText = other.content.toString()).apply {
        editor = other.editor
        content = other.content
    }
}
