package com.klyx.ui.palette

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.runtime.Platform
import com.klyx.runtime.currentPlatform
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary

internal val LocalShortcutRecording = staticCompositionLocalOf { mutableStateOf(false) }

@Composable
internal fun ShortcutSettings(
    bindings: Map<String, String>,
    change: (EditorCommand, String) -> Unit
) {
    var editing by remember { mutableStateOf<EditorCommand?>(null) }

    Column {
        for (command in EditorCommand.entries) {
            KxButton(
                onClick = { editing = command },
                variant = KxButtonVariant.Ghost
            ) {
                Text("${command.title} · ${command.binding(bindings).ifEmpty { "Unassigned" }}")
            }
        }
    }

    editing?.let { command ->
        val recording = LocalShortcutRecording.current

        DisposableEffect(Unit) {
            recording.value = true
            onDispose { recording.value = false }
        }

        var chord by remember(command) { mutableStateOf(command.binding(bindings)) }
        val parts = chord.split('+')
        val modifiers = parts.filter { it in shortcutModifiers }
        val key = parts.lastOrNull { it in shortcutKeys }
        val candidate = normalizeBinding(chord)

        var showKeys by remember { mutableStateOf(false) }
        val focus = remember { FocusRequester() }

        val conflict = EditorCommand.entries.find {
            it != command && !candidate.isNullOrEmpty() && it.binding(bindings) == candidate
        }

        KxDialog(
            visible = true,
            onDismissRequest = { editing = null },
            title = command.title,
            subtitle = "Press a shortcut on your keyboard.",
            modifier = Modifier.onPreviewKeyEvent { event ->
                val pressed = event.chord() ?: return@onPreviewKeyEvent false
                if (event.type == KeyEventType.KeyDown) chord = pressed
                true
            },
            actions = {
                KxButton(
                    onClick = { editing = null },
                    variant = KxButtonVariant.Ghost
                ) {
                    Text("Cancel")
                }
                KxButton(
                    enabled = candidate != null && conflict == null,
                    onClick = {
                        candidate?.let { change(command, it) }
                        editing = null
                    }
                ) {
                    Text("Save")
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .focusable()
            ) {
                LaunchedEffect(Unit) {
                    focus.requestFocus()
                }

                Text(chord.ifEmpty { "Unassigned" }, color = Theme[colors][textPrimary])

                if (conflict != null) Text("Assigned to ${conflict.title}", color = Theme[colors][textMuted])

                if (candidate == null) {
                    Text(
                        if (key == null) "Choose a key." else "Choose ctrl or alt for a letter or arrow.",
                        color = Theme[colors][textMuted]
                    )
                }

                Row {
                    KxButton(
                        onClick = { chord = "" },
                        variant = KxButtonVariant.Ghost
                    ) {
                        Text("Clear")
                    }

                    if (currentPlatform == Platform.Android || currentPlatform == Platform.iOS) {
                        KxButton(
                            onClick = { showKeys = !showKeys },
                            variant = KxButtonVariant.Ghost
                        ) {
                            Text("Special keys")
                        }
                    }
                }

                if (showKeys) {
                    FlowRow(
                        modifier = Modifier
                            .heightIn(max = 220.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        shortcutModifiers.forEach { mod ->
                            KxButton(
                                onClick = {
                                    val selected = if (mod in modifiers) modifiers - mod else modifiers + mod
                                    chord = (shortcutModifiers
                                        .filter { it in selected } + listOfNotNull(key))
                                        .joinToString("+")
                                },
                                variant = if (mod in modifiers) KxButtonVariant.Filled else KxButtonVariant.Outlined
                            ) {
                                Text(mod)
                            }
                        }

                        shortcutKeys.forEach { selectedKey ->
                            KxButton(
                                onClick = {
                                    chord = (modifiers + selectedKey).joinToString("+")
                                },
                                variant = KxButtonVariant.Ghost
                            ) {
                                Text(selectedKey)
                            }
                        }
                    }
                }
            }
        }
    }
}
