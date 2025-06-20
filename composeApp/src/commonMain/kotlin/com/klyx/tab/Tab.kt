package com.klyx.tab

import androidx.compose.runtime.Composable
import com.klyx.core.file.FileWrapper
import com.klyx.editor.EditorState
import kotlinx.datetime.Clock

sealed class Tab(
    open val name: String,
    open val id: String = "${Clock.System.now().toEpochMilliseconds()}",
    open val data: Any? = null,
    open val content: @Composable () -> Unit,
) {
    data class AnyTab(
        override val name: String,
        override val id: String = "${Clock.System.now().toEpochMilliseconds()}",
        override val data: Any? = null,
        override val content: @Composable () -> Unit,
    ) : Tab(name, id, data, content)

    data class FileTab(
        override val name: String,
        override val id: String = "${Clock.System.now().toEpochMilliseconds()}",
        val fileWrapper: FileWrapper,
        val editorState: EditorState,
        val isInternal: Boolean = false,
        override val content: @Composable () -> Unit,
    ) : Tab(name, id, fileWrapper, content)
}
