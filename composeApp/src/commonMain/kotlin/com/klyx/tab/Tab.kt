package com.klyx.tab

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.klyx.core.file.FileWrapper
import com.klyx.editor.CodeEditorState
import com.klyx.editor.ExperimentalCodeEditorApi
import kotlinx.datetime.Clock

typealias TabId = String

sealed class Tab(
    open val name: String,
    open val id: TabId = "${Clock.System.now().toEpochMilliseconds()}",
    open val data: Any? = null,
    open val content: @Composable () -> Unit,
) {
    data class AnyTab(
        override val name: String,
        override val id: TabId = "${Clock.System.now().toEpochMilliseconds()}",
        override val data: Any? = null,
        override val content: @Composable () -> Unit,
    ) : Tab(name, id, data, content)

    @OptIn(ExperimentalCodeEditorApi::class)
    @Stable
    data class FileTab(
        override val name: String,
        val fileWrapper: FileWrapper,
        val editorState: CodeEditorState,
        val isInternal: Boolean = false,
        override val id: TabId = "${Clock.System.now().toEpochMilliseconds()}",
        override val content: @Composable () -> Unit = { Box(modifier = Modifier.fillMaxSize()) },
    ) : Tab(name, id, fileWrapper, content) {

        var isModified by mutableStateOf(false)

        init {
            editorState.addTextChangedListener {
                //println(it)
                isModified = true
            }
        }

        fun markAsSaved() {
            isModified = false
        }
    }
}
