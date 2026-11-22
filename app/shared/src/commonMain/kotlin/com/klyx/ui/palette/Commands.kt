package com.klyx.ui.palette

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import com.klyx.core.action.Action
import com.klyx.runtime.action.ActivateNextDocument
import com.klyx.runtime.action.ActivatePreviousDocument
import com.klyx.runtime.action.CloseActiveDocument
import com.klyx.runtime.action.FoldAll
import com.klyx.runtime.action.Redo
import com.klyx.runtime.action.SaveActiveDocument
import com.klyx.runtime.action.SaveAllDocuments
import com.klyx.runtime.action.SplitEditor
import com.klyx.runtime.action.Undo
import com.klyx.runtime.action.UnfoldAll

internal val LocalRunEditorCommand = staticCompositionLocalOf<(EditorCommand) -> Unit> {
    error("Missing editor command handler")
}

internal enum class EditorCommand(val title: String, val defaultBinding: String = "", val action: Action? = null) {
    QuickOpen("Find file", "ctrl+p"),
    FindInFiles("Find in files", "ctrl+shift+f"),
    Definition("Go to definition", "f12"),
    References("Find all references", "shift+f12"),
    Back("Go back", "alt+left"),
    Palette("Command palette", "ctrl+shift+p"),
    Settings("Open settings"),
    Save("Save file", "ctrl+s", SaveActiveDocument),
    SaveAll("Save all files", "ctrl+shift+s", SaveAllDocuments),
    Split("Split editor", action = SplitEditor),
    Close("Close active file", action = CloseActiveDocument),
    Next("Next tab", action = ActivateNextDocument),
    Previous("Previous tab", action = ActivatePreviousDocument),
    UndoEdit("Undo", action = Undo),
    RedoEdit("Redo", action = Redo),
    Fold("Fold all", action = FoldAll),
    Unfold("Unfold all", action = UnfoldAll),
}

private val letters = listOf(
    Key.A,
    Key.B,
    Key.C,
    Key.D,
    Key.E,
    Key.F,
    Key.G,
    Key.H,
    Key.I,
    Key.J,
    Key.K,
    Key.L,
    Key.M,
    Key.N,
    Key.O,
    Key.P,
    Key.Q,
    Key.R,
    Key.S,
    Key.T,
    Key.U,
    Key.V,
    Key.W,
    Key.X,
    Key.Y,
    Key.Z
)

private val keys = buildMap {
    letters.forEachIndexed { index, key -> put(key, ('a' + index).toString()) }
    listOf(Key.F1, Key.F2, Key.F3, Key.F4, Key.F5, Key.F6, Key.F7, Key.F8, Key.F9, Key.F10, Key.F11, Key.F12)
        .forEachIndexed { index, key -> put(key, "f${index + 1}") }
    put(Key.DirectionLeft, "left")
    put(Key.DirectionRight, "right")
    put(Key.DirectionUp, "up")
    put(Key.DirectionDown, "down")
}

internal val shortcutKeys = keys.values.toList()
internal val shortcutModifiers = listOf("ctrl", "alt", "shift")

internal fun KeyEvent.chord(): String? {
    val name = keys[key] ?: return null
    return buildList {
        if (isCtrlPressed || isMetaPressed) add("ctrl")
        if (isAltPressed) add("alt")
        if (isShiftPressed) add("shift")
        add(name)
    }.joinToString("+")
}

internal fun KeyEvent.binding(): String? = chord()?.let(::normalizeBinding)

internal fun normalizeBinding(input: String): String? {
    if (input.isBlank()) return ""
    val parts = input.lowercase().split('+').map { it.trim() }
    val key = parts.last()
    val mods = parts.dropLast(1).map { if (it == "cmd") "ctrl" else it }
    if (key !in shortcutKeys || mods.any { it !in shortcutModifiers } || mods.distinct() != mods) return null
    val functionKey = key.startsWith("f") && key.length > 1
    if (!functionKey && "ctrl" !in mods && "alt" !in mods) return null
    return (shortcutModifiers.filter { it in mods } + key).joinToString("+")
}

internal fun EditorCommand.binding(bindings: Map<String, String>) =
    bindings[name]?.let(::normalizeBinding) ?: defaultBinding
