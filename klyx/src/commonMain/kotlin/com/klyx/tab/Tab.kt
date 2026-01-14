@file:OptIn(ExperimentalCodeEditorApi::class)

package com.klyx.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.klyx.core.file.KxFile
import com.klyx.core.generateId
import com.klyx.editor.ComposeEditorState
import com.klyx.editor.EditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.SoraEditorState
import com.klyx.editor.compose.event.TextChangeEvent
import com.klyx.editor.event.ContentChangeEvent
import com.klyx.project.Worktree

typealias TabId = String

@Stable
sealed class Tab(
    open val name: String,
    open val id: TabId = generateId(),
    open val data: Any? = null
)

@Stable
data class ComposableTab(
    override val name: String,
    override val id: TabId = generateId(),
    override val data: Any? = null,
    val content: @Composable () -> Unit,
) : Tab(name, id, data)

@Stable
data class UnsupportedFileTab(
    override val name: String,
    val file: KxFile,
    override val id: TabId = generateId()
) : Tab(name, id, file)

@Stable
data class FileTab(
    override val name: String,
    val file: KxFile,
    val editorState: EditorState,
    val worktree: Worktree? = null,
    val isInternal: Boolean = false,
    override val id: TabId = generateId()
) : Tab(name, id, file) {

    inline val isReadOnly get() = isInternal

    var isModified by mutableStateOf(false)

    init {
        when (editorState) {
            is ComposeEditorState -> {
                editorState.state.subscribeEvent<TextChangeEvent> {
                    isModified = true
                }
            }

            is SoraEditorState -> {
                editorState.state.subscribeEvent<ContentChangeEvent> {
                    isModified = true
                }
            }
        }
    }

    fun markAsSaved() {
        isModified = false
    }
}
