package com.klyx.editor.compose.selection.contextmenu

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
internal expect value class ContextMenuIcons(val value: Int) {
    companion object {
        val ActionModeCutDrawable: ContextMenuIcons
        val ActionModeCopyDrawable: ContextMenuIcons
        val ActionModePasteDrawable: ContextMenuIcons
        val ActionModeSelectAllDrawable: ContextMenuIcons
        val ID_NULL: ContextMenuIcons
    }
}
