package com.klyx.editor.compose

import androidx.compose.runtime.compositionLocalOf
import com.klyx.core.noLocalProvidedFor
import com.klyx.viewmodel.EditorViewModel

val LocalEditorViewModel = compositionLocalOf<EditorViewModel> {
    noLocalProvidedFor<EditorViewModel>()
}
