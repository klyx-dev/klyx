package com.klyx.presentation.screen.settings.terminal

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.klyx.api.data.preferences.TerminalSettings as TerminalSettingsPrefs
import com.klyx.app.icons.Keyboard
import com.klyx.data.preferences.updateTerminalSettings
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.terminal.ui.extrakeys.ExtraKeyStyle
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun KeyboardSettingsSection(
    settings: TerminalSettingsPrefs,
    scope: CoroutineScope
) {
    SettingsSubsection(title = strings.keyboardSection) {
        val s = strings
        SelectorItem(
            label = strings.extraKeysStyle,
            description = strings.extraKeysStyleDesc,
            options = ExtraKeyStyle.entries.toImmutableList(),
            selected = settings.extraKeysStyle,
            optionLabel = { style ->
                when (style) {
                    ArrowsOnly -> s.extraArrowsOnly
                    ArrowsAll -> s.extraArrowsAll
                    All -> s.all
                    None -> s.extraNone
                    Default -> s.extraDefault
                }
            },
            optionDescription = { style ->
                when (style) {
                    ArrowsOnly -> s.extraArrowsOnlyDesc
                    ArrowsAll -> s.extraArrowsAllDesc
                    All -> s.extraAllDesc
                    None -> s.extraNoneDesc
                    Default -> s.extraDefaultDesc
                }
            },
            onSelectionChanged = { selectedStyle ->
                scope.launch {
                    updateTerminalSettings { copy(extraKeysStyle = selectedStyle) }
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Keyboard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )
    }
}
