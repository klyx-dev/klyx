package com.klyx.presentation.screen.settings.terminal

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.klyx.api.data.preferences.TerminalSettings as TerminalSettingsPrefs
import com.klyx.api.util.sliderSteps
import com.klyx.app.icons.FlashOn
import com.klyx.app.icons.TextFormat
import com.klyx.data.preferences.updateTerminalSettings
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import com.klyx.terminal.emulator.CursorStyle
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun TextSettingsSection(
    settings: TerminalSettingsPrefs,
    scope: CoroutineScope
) {
    SettingsSubsection(title = strings.textSection) {
        SliderSettingsItem(
            label = strings.fontSize,
            value = settings.fontSize,
            valueRange = 8f..30f,
            steps = (8f..30f).sliderSteps(1f),
            onValueChange = { size ->
                scope.launch {
                    updateTerminalSettings { copy(fontSize = size) }
                }
            },
            valueText = { "${it.toInt()}sp" }
        )

        SwitchSettingItem(
            title = strings.cursorBlinking,
            subtitle = strings.cursorBlinkingDesc,
            checked = settings.cursorBlink,
            onCheckedChange = { blink ->
                scope.launch {
                    updateTerminalSettings { copy(cursorBlink = blink) }
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )

        val s = strings
        SelectorItem(
            label = strings.cursorStyle,
            description = strings.cursorStyleDesc,
            options = CursorStyle.availableStyles().toImmutableList(),
            selected = settings.cursorStyle,
            optionLabel = { style ->
                when (style) {
                    CursorStyle.Block -> s.cursorBlock
                    CursorStyle.Underline -> s.cursorUnderline
                    CursorStyle.Bar -> s.cursorBar
                    else -> s.unknown
                }
            },
            optionDescription = { style ->
                when (style) {
                    CursorStyle.Block -> s.cursorBlockDesc
                    CursorStyle.Underline -> s.cursorUnderlineDesc
                    CursorStyle.Bar -> s.cursorBarDesc
                    else -> null
                }
            },
            onSelectionChanged = { selectedStyle ->
                scope.launch {
                    updateTerminalSettings { copy(cursorStyle = selectedStyle) }
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.TextFormat,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )
    }
}
