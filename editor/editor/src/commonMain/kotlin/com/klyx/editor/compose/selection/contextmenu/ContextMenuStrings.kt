package com.klyx.editor.compose.selection.contextmenu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import kotlin.jvm.JvmInline

@Immutable
@JvmInline
internal expect value class ContextMenuStrings(val value: Int) {
    companion object {
        val Cut: ContextMenuStrings
        val Copy: ContextMenuStrings
        val Paste: ContextMenuStrings
        val SelectAll: ContextMenuStrings
        val Autofill: ContextMenuStrings
    }
}

@Composable @ReadOnlyComposable internal expect fun getString(string: ContextMenuStrings): String
