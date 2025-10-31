package com.klyx.editor

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.klyx.core.file.KxFile
import com.klyx.editor.event.ContentChangeEvent
import io.github.rosemoe.sora.event.Event
import io.github.rosemoe.sora.event.SelectionChangeEvent
import io.github.rosemoe.sora.event.SubscriptionReceipt
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.subscribeAlways
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.reflect.KProperty
import com.klyx.editor.event.Event as KlyxEvent

@Stable
@ExperimentalCodeEditorApi
actual class CodeEditorState actual constructor(
    actual val file: KxFile,
    actual val project: KxFile?
) {
    private val subscriptions = mutableListOf<(KlyxEditor) -> Unit>()

    var editor: KlyxEditor? = null
        set(value) {
            field = value
            if (value != null) {
                subscriptions.forEach { attach -> attach(value) }
            }
        }

    var content by mutableStateOf(Content(runCatching { file.readText() }.getOrElse { "" }))

    private val _cursor = MutableStateFlow(CursorState())
    actual val cursor = _cursor.asStateFlow()

    init {
        addSubscription { editor ->
            editor.subscribeAlways<SelectionChangeEvent> { event ->
                val cursor = event.editor.cursor
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
    }

    @PublishedApi
    internal fun addSubscription(attach: (KlyxEditor) -> Unit) {
        subscriptions += attach
        editor?.let { attach(it) }
    }

    inline fun <reified T : Event> subscribeAlways(
        noinline onEvent: (T) -> Unit
    ): SubscriptionReceipt<T>? {
        var receipt: SubscriptionReceipt<T>? = null
        addSubscription { ed -> receipt = ed.subscribeAlways(onEvent) }
        return receipt
    }

    fun setLanguage(language: Language) {
        editor?.setEditorLanguage(language)
    }

    @PublishedApi
    internal fun editorNotInitialized(): Nothing {
        error("Editor not initialized")
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

    actual fun canUndo(): Boolean = editor?.canUndo() == true

    actual fun canRedo(): Boolean = editor?.canRedo() == true

    actual fun undo(): Boolean {
        editor?.undo()
        return canUndo()
    }

    actual fun redo(): Boolean {
        editor?.redo()
        return canRedo()
    }
}

@ExperimentalCodeEditorApi
actual fun CodeEditorState(other: CodeEditorState): CodeEditorState {
    return CodeEditorState(
        file = other.file,
        project = other.project
    ).apply {
        editor = other.editor
        content = other.content
    }
}
