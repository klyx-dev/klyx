package com.klyx.ui.editor

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import com.klyx.runtime.settings.SettingsStore
import com.klyx.ui.palette.EditorCommand
import com.klyx.ui.palette.LocalRunEditorCommand
import com.klyx.ui.palette.binding
import com.klyx.ui.palette.chord
import com.klyx.ui.palette.normalizeBinding
import org.koin.compose.koinInject
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.core.action.action
import com.klyx.editor.CursorDirection
import com.klyx.editor.changeIndent
import com.klyx.editor.insertTab
import com.klyx.editor.lineCommentPrefix
import com.klyx.editor.moveCursor
import com.klyx.editor.toggleLineComment
import com.klyx.lsp.EditorLsp
import com.klyx.lsp.LspDocumentUtils
import com.klyx.lsp.applyCompletion
import com.klyx.lsp.formatDocument
import com.klyx.lsp.triggerCompletionPopup
import com.klyx.runtime.action.SaveDocument
import com.klyx.runtime.workspace.Document
import com.klyx.ui.theme.accent
import com.klyx.ui.theme.border
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.kxSmallShape
import com.klyx.ui.theme.label
import com.klyx.ui.theme.surfaceVariant
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.typography
import io.github.lumkit.sweeteditor.SweetEditorController
import kotlinx.coroutines.launch

private enum class InputModifier { Ctrl, Alt, Shift }

internal fun mobileBinding(modifiers: List<String>, key: String, shift: Boolean): String? {
    val active = if (shift && "shift" !in modifiers) modifiers + "shift" else modifiers
    return normalizeBinding((active + key).joinToString("+"))
}

private enum class InputPanel { Symbols, Edit }

private enum class Shortcut(
    val modifier: InputModifier,
    val key: Key,
    val label: String,
    val description: String,
) {
    Save(InputModifier.Ctrl, Key.S, "S", "Save"),
    Undo(InputModifier.Ctrl, Key.Z, "Z", "Undo"),
    Redo(InputModifier.Ctrl, Key.Y, "Y", "Redo"),
    Cut(InputModifier.Ctrl, Key.X, "X", "Cut"),
    Copy(InputModifier.Ctrl, Key.C, "C", "Copy"),
    Paste(InputModifier.Ctrl, Key.V, "V", "Paste"),
    SelectAll(InputModifier.Ctrl, Key.A, "A", "Select all"),
    Comment(InputModifier.Ctrl, Key.Slash, "/", "Toggle line comment"),
    Complete(InputModifier.Ctrl, Key.Spacebar, "Space", "Show completions"),
    MoveUp(InputModifier.Alt, Key.DirectionUp, "↑", "Move line up"),
    MoveDown(InputModifier.Alt, Key.DirectionDown, "↓", "Move line down"),
    PreviousWord(InputModifier.Alt, Key.DirectionLeft, "←", "Previous word"),
    NextWord(InputModifier.Alt, Key.DirectionRight, "→", "Next word"),
}

private class MobileInputState {
    var modifiers by mutableStateOf(emptySet<InputModifier>())
    var panel by mutableStateOf<InputPanel?>(null)

    fun toggleModifier(value: InputModifier) {
        modifiers = if (value in modifiers) modifiers - value else modifiers + value
    }

    fun togglePanel(value: InputPanel) {
        modifiers = emptySet()
        panel = if (panel == value) null else value
    }
}

@Composable
internal fun MobileEditorInput(
    document: Document,
    controller: SweetEditorController,
    lsp: EditorLsp,
    enabled: Boolean,
    focusEditor: () -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (layout: Modifier, keys: Modifier) -> Unit,
) {
    val settings by koinInject<SettingsStore>().settings.collectAsState()
    val bindings = remember(settings.keybindings) { EditorCommand.entries.associateWith { it.binding(settings.keybindings) } }
    val dispatch = LocalRunEditorCommand.current

    val input = remember(controller, enabled) { MobileInputState() }
    var hasSelection by remember(controller) { mutableStateOf(controller.getSelection() != null) }
    var canUndo by remember(controller) { mutableStateOf(controller.canUndo()) }
    var canRedo by remember(controller) { mutableStateOf(controller.canRedo()) }
    var formatting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val commentPrefix = lineCommentPrefix(LspDocumentUtils.languageIdOf(document))
    val save = action(SaveDocument(document.id))

    if (enabled) DisposableEffect(controller) {
        hasSelection = controller.getSelection() != null
        canUndo = controller.canUndo()
        canRedo = controller.canRedo()
        val unsubscribeSelection = controller.onSelectionChanged { hasSelection = it.hasSelection }
        val unsubscribeText = controller.onTextChanged {
            input.modifiers = emptySet()
            canUndo = controller.canUndo()
            canRedo = controller.canRedo()
        }
        onDispose {
            unsubscribeSelection()
            unsubscribeText()
        }
    }

    fun edit(block: SweetEditorController.() -> Unit) {
        input.modifiers = emptySet()
        if (controller.getSettings().readOnly) return
        controller.block()
    }

    fun escape() {
        if (input.modifiers.isNotEmpty() || input.panel != null) {
            input.modifiers = emptySet()
            input.panel = null
            return
        }
        val cursor = controller.getCursorPosition()
        when {
            lsp.activeCompletion.value?.documentId == document.id -> lsp.dismissCompletionPopup()
            controller.isInlineSuggestionShowing() -> controller.dismissInlineSuggestion()
            controller.isInLinkedEditing() -> controller.cancelLinkedEditing()
            lsp.activeHover.value != null -> lsp.dismissHover()
            controller.getSelection() != null && cursor != null -> controller.setCursorPosition(cursor)
        }
        controller.dismissCompletion()
    }

    fun shortcutEnabled(shortcut: Shortcut): Boolean = when (shortcut) {
        Shortcut.Undo -> canUndo
        Shortcut.Redo -> canRedo
        Shortcut.Cut, Shortcut.Copy -> hasSelection
        Shortcut.Comment -> commentPrefix != null
        else -> true
    }

    fun runShortcut(shortcut: Shortcut) {
        input.modifiers = emptySet()
        if (!shortcutEnabled(shortcut)) return
        when (shortcut) {
            Shortcut.Save -> save()
            Shortcut.Copy -> {
                controller.copyToClipboard()
            }

            Shortcut.SelectAll -> controller.selectAll()
            Shortcut.Complete -> lsp.triggerCompletionPopup(document, controller)
            Shortcut.PreviousWord -> controller.moveCursor(CursorDirection.Left, byWord = true)
            Shortcut.NextWord -> controller.moveCursor(CursorDirection.Right, byWord = true)
            Shortcut.Undo -> edit { undo() }
            Shortcut.Redo -> edit { redo() }
            Shortcut.Cut -> edit { cutToClipboard() }
            Shortcut.Paste -> edit { pasteFromClipboard() }
            Shortcut.Comment -> edit { commentPrefix?.let { toggleLineComment(it) } }
            Shortcut.MoveUp -> edit { moveLineUp() }
            Shortcut.MoveDown -> edit { moveLineDown() }
        }
    }

    fun runCommand(command: EditorCommand) {
        input.modifiers = emptySet()
        focusEditor()
        dispatch(command)
    }

    fun runArmedKey(name: String, shift: Boolean = false): Boolean {
        val mods = input.modifiers
        if (mods.isEmpty()) return false
        val binding = mobileBinding(mods.map { it.name.lowercase() }, name, shift)
        val command = bindings.entries.firstOrNull { it.value == binding }?.key
        if (command != null) {
            runCommand(command)
            return true
        }
        val shortcut = Shortcut.entries.firstOrNull {
            it != Shortcut.Save && mods == setOf(it.modifier) && it.label.equals(name, ignoreCase = true)
        } ?: return false
        focusEditor()
        runShortcut(if (shift && shortcut == Shortcut.Undo) Shortcut.Redo else shortcut)
        return true
    }

    fun move(direction: CursorDirection, key: Key) {
        if (runArmedKey(direction.name.lowercase())) return
        val armed = input.modifiers.singleOrNull()
        val shortcut = Shortcut.entries.firstOrNull { it.modifier == armed && it.key == key }
        input.modifiers = emptySet()
        focusEditor()
        if (shortcut != null) runShortcut(shortcut)
        else controller.moveCursor(direction, byWord = armed == InputModifier.Ctrl)
    }

    fun handleKey(event: KeyEvent): Boolean {
        if (input.modifiers.isEmpty() || !enabled || event.type != KeyEventType.KeyDown) return false
        when (event.key) {
            Key.Escape -> { escape(); focusEditor() }
            Key.DirectionLeft -> move(CursorDirection.Left, event.key)
            Key.DirectionRight -> move(CursorDirection.Right, event.key)
            Key.DirectionUp -> move(CursorDirection.Up, event.key)
            Key.DirectionDown -> move(CursorDirection.Down, event.key)
            else -> {
                val name = event.chord()?.substringAfterLast('+') ?: Shortcut.entries.find { it.key == event.key }?.label ?: return false
                return runArmedKey(name, event.isShiftPressed)
            }
        }
        return true
    }

    fun tab() {
        val completion = lsp.activeCompletion.value?.takeIf { it.documentId == document.id }
        val item = completion?.items?.getOrNull(completion.selectedIndex)
        edit {
            if (item != null) lsp.applyCompletion(item, this)
            else insertTab()
        }
    }

    Column(modifier = modifier) {
        content(Modifier
            .weight(1f)
            .fillMaxWidth(), Modifier.onPreviewKeyEvent(::handleKey))

        if (enabled) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Theme[colors][surfaceVariant])
            ) {
                val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
                val rowHeight = Modifier.height(if (keyboardVisible) 36.dp else 48.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Theme[colors][border])
                )

                Row(
                    modifier = rowHeight,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KeyRow(Modifier.weight(1f)) {
                        for (mod in listOf(InputModifier.Ctrl, InputModifier.Shift, InputModifier.Alt)) {
                            InputKey(mod.name, selected = mod in input.modifiers) {
                                input.toggleModifier(mod)
                                if (input.modifiers.isEmpty()) focusEditor()
                            }
                        }
                        InputKey("Esc", "Escape", onClick = ::escape)
                        InputKey("Tab", onClick = ::tab)
                        InputKey("←", "Left") { move(CursorDirection.Left, Key.DirectionLeft) }
                        InputKey("↑", "Up") { move(CursorDirection.Up, Key.DirectionUp) }
                        InputKey("↓", "Down") { move(CursorDirection.Down, Key.DirectionDown) }
                        InputKey("→", "Right") { move(CursorDirection.Right, Key.DirectionRight) }
                    }
                    InputKey("{ }", "Symbols", selected = input.panel == InputPanel.Symbols) {
                        input.togglePanel(InputPanel.Symbols)
                    }
                    InputKey("Edit", "Editing actions", selected = input.panel == InputPanel.Edit) {
                        input.togglePanel(InputPanel.Edit)
                    }
                }

                val armed = input.modifiers
                val panel = input.panel ?: InputPanel.Edit.takeIf { hasSelection }
                key(armed.isNotEmpty(), panel) {
                    when {
                        armed.isNotEmpty() -> KeyRow(rowHeight) {
                            val prefix = listOf(InputModifier.Ctrl, InputModifier.Alt, InputModifier.Shift)
                                .filter { it in armed }
                                .joinToString("+") { it.name.lowercase() }
                            val commands = bindings.filterValues {
                                it.substringBeforeLast('+', "") == prefix
                            }
                            val focus = remember { FocusRequester() }

                            BasicTextField(
                                value = "",
                                onValueChange = { text ->
                                    text.singleOrNull()?.let { ch ->
                                        val _ = runArmedKey(if (ch == ' ') "Space" else ch.lowercase(), ch.isUpperCase())
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Ascii,
                                    autoCorrectEnabled = false
                                ),
                                modifier = Modifier
                                    .widthIn(min = 32.dp)
                                    .focusRequester(focus)
                                    .onPreviewKeyEvent(::handleKey)
                                    .semantics { contentDescription = "Shortcut key" },
                                decorationBox = { inner ->
                                    Box { Text("Key", color = Theme[colors][textMuted]); inner() }
                                },
                            )

                            LaunchedEffect(Unit) {
                                focus.requestFocus()
                            }

                            for ((key, value) in commands) {
                                val name = value.substringAfterLast('+')
                                InputKey(name.uppercase(), key.title) { runCommand(key) }
                            }

                            val commandKeys = commands.values.map {
                                it.substringAfterLast('+')
                            }.toSet()

                            Shortcut.entries.filter {
                                it != Shortcut.Save && armed == setOf(it.modifier) && it.label.lowercase() !in commandKeys
                            }.forEach { shortcut ->
                                InputKey(shortcut.label, shortcut.description, enabled = shortcutEnabled(shortcut)) {
                                    focusEditor()
                                    runShortcut(shortcut)
                                }
                            }
                        }

                        panel == InputPanel.Symbols -> KeyRow(rowHeight) {
                            for (symbol in "{}[]()<>/\\=+-_*&|!?:;.,'\"`~#@%^$") {
                                InputKey(symbol.toString()) { edit { insertText(symbol.toString()) } }
                            }
                        }

                        panel == InputPanel.Edit -> KeyRow(rowHeight) {
                            InputKey("Cut", enabled = hasSelection) { runShortcut(Shortcut.Cut) }
                            InputKey("Copy", enabled = hasSelection) { runShortcut(Shortcut.Copy) }
                            InputKey("Paste") { runShortcut(Shortcut.Paste) }
                            InputKey("Undo", enabled = canUndo) { runShortcut(Shortcut.Undo) }
                            InputKey("Redo", enabled = canRedo) { runShortcut(Shortcut.Redo) }
                            InputKey("Save") { runShortcut(Shortcut.Save) }
                            InputKey("Indent") { edit { changeIndent(outdent = false) } }
                            InputKey("Outdent") { edit { changeIndent(outdent = true) } }
                            InputKey("Duplicate", "Duplicate line") { edit { copyLineDown() } }
                            InputKey("Comment", "Toggle line comment", enabled = commentPrefix != null) {
                                runShortcut(Shortcut.Comment)
                            }
                            InputKey("Format", enabled = !formatting) {
                                val selection = controller.getSelection()
                                formatting = true
                                scope.launch {
                                    try {
                                        val formatted = lsp.formatDocument(document, controller, selection)
                                        if (!formatted && selection != null) {
                                            onShowToast("Could not format the selection. Check the language server and try again.")
                                        }
                                    } finally {
                                        formatting = false
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyRow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content()
    }
}

@Composable
private fun InputKey(
    text: String,
    description: String = text,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 44.dp)
            .clip(kxSmallShape())
            .background(Theme[colors][accent].copy(alpha = if (selected) 0.18f else 0f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics {
                if (description != text) contentDescription = description
                if (selected) this.selected = true
            }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = Theme[typography][label],
            color = when {
                !enabled -> Theme[colors][textMuted]
                selected -> Theme[colors][accent]
                else -> Theme[colors][textPrimary]
            },
        )
    }
}
