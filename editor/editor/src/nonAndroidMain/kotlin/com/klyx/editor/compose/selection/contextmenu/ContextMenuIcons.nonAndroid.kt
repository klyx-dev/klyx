package com.klyx.editor.compose.selection.contextmenu

import androidx.compose.runtime.Immutable
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
internal actual value class ContextMenuIcons actual constructor(actual val value: Int) {
    actual companion object {
        actual val ActionModeCutDrawable = ContextMenuIcons(0)
        actual val ActionModeCopyDrawable = ContextMenuIcons(1)
        actual val ActionModePasteDrawable = ContextMenuIcons(2)
        actual val ActionModeSelectAllDrawable = ContextMenuIcons(3)
        actual val ID_NULL = ContextMenuIcons(-1)
    }
}
