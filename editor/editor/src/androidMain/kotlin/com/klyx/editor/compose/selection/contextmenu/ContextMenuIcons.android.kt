package com.klyx.editor.compose.selection.contextmenu

//noinspection SuspiciousImport
import android.R
import androidx.compose.runtime.Immutable

@Immutable
@JvmInline
internal actual value class ContextMenuIcons actual constructor(actual val value: Int) {
    actual companion object {
        actual val ActionModeCutDrawable: ContextMenuIcons
            get() = ContextMenuIcons(R.attr.actionModeCutDrawable)

        actual val ActionModeCopyDrawable: ContextMenuIcons
            get() = ContextMenuIcons(R.attr.actionModeCopyDrawable)

        actual val ActionModePasteDrawable: ContextMenuIcons
            get() = ContextMenuIcons(R.attr.actionModePasteDrawable)

        actual val ActionModeSelectAllDrawable: ContextMenuIcons
            get() = ContextMenuIcons(R.attr.actionModeSelectAllDrawable)

        actual val ID_NULL: ContextMenuIcons
            get() = ContextMenuIcons(0)
    }
}
