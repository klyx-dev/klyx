package com.klyx.editor.compose.selection.contextmenu

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalResources
import com.klyx.editor.R

@Immutable
@JvmInline
internal actual value class ContextMenuStrings actual constructor(actual val value: Int) {
    actual companion object {
        actual val Cut: ContextMenuStrings
            get() = ContextMenuStrings(android.R.string.cut)

        actual val Copy: ContextMenuStrings
            get() = ContextMenuStrings(android.R.string.copy)

        actual val Paste: ContextMenuStrings
            get() = ContextMenuStrings(android.R.string.paste)

        actual val SelectAll: ContextMenuStrings
            get() = ContextMenuStrings(android.R.string.selectAll)

        actual val Autofill: ContextMenuStrings
            get() =
                ContextMenuStrings(
                    if (Build.VERSION.SDK_INT <= 26) {
                        R.string.klyx_autofill
                    } else {
                        android.R.string.autofill
                    }
                )
    }
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: ContextMenuStrings): String {
    val resources = LocalResources.current
    return resources.getString(string.value)
}
