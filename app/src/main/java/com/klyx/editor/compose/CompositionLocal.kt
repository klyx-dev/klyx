package com.klyx.editor.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.klyx.core.file.FileId
import com.klyx.core.noLocalProvidedFor
import com.klyx.editor.KlyxCodeEditor
import com.klyx.viewmodel.EditorViewModel

val LocalEditorStore = compositionLocalOf<SnapshotStateMap<FileId, KlyxCodeEditor>> {
    noLocalProvidedFor("EditorStore")
}

val LocalEditorViewModel = compositionLocalOf<EditorViewModel> {
    noLocalProvidedFor<EditorViewModel>()
}
