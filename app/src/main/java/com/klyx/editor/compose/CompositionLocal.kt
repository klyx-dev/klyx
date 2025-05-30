package com.klyx.editor.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.klyx.core.file.FileId
import com.klyx.editor.KlyxCodeEditor
import com.klyx.viewmodel.EditorViewModel

val LocalEditorStore = compositionLocalOf<SnapshotStateMap<FileId, KlyxCodeEditor>> {
    error("EditorStore not provided")
}

val LocalEditorViewModel = compositionLocalOf<EditorViewModel> {
    error("EditorViewModel not provided")
}
