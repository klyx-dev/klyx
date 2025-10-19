package com.klyx.editor.compose.selection.contextmenu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
internal actual value class ContextMenuStrings actual constructor(actual val value: Int) {
    actual companion object {
        actual val Cut = ContextMenuStrings(0)
        actual val Copy = ContextMenuStrings(1)
        actual val Paste = ContextMenuStrings(2)
        actual val SelectAll = ContextMenuStrings(3)
        actual val Autofill = ContextMenuStrings(4)
    }
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: ContextMenuStrings): String {
    return when (string.value) {
        0 -> "Cut"
        1 -> "Copy"
        2 -> "Paste"
        3 -> "Select all"
        4 -> "Autofill"
        else -> error("Invalid string: $string")
    }
}
