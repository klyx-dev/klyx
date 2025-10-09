@file:Suppress("NOTHING_TO_INLINE")

package com.klyx.tab

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.klyx.core.file.KxFile
import com.klyx.core.generateId
import com.klyx.editor.compose.CodeEditorState
import com.klyx.extension.api.Worktree

typealias TabId = String

sealed class Tab(
    open val name: String,
    open val id: TabId = generateId(),
    open val data: Any? = null
) {
    data class AnyTab(
        override val name: String,
        override val id: TabId = generateId(),
        override val data: Any? = null,
        val content: @Composable () -> Unit,
    ) : Tab(name, id, data)

    @Stable
    data class FileTab(
        override val name: String,
        val file: KxFile,
        val editorState: CodeEditorState,
        val worktree: Worktree? = null,
        val isInternal: Boolean = false,
        override val id: TabId = generateId()
    ) : Tab(name, id, file) {

        var isModified by mutableStateOf(false)

        init {
//            editorState.subscribeEvent<ContentChangeEvent> {
//                isModified = true
//            }
        }

        fun markAsSaved() {
            isModified = false
        }
    }
}

inline fun Tab.isFileTab() = this is Tab.FileTab
