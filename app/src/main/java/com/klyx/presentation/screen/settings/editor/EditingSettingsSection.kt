package com.klyx.presentation.screen.settings.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.app.icons.Backspace
import com.klyx.app.icons.ContentPaste
import com.klyx.app.icons.DataArray
import com.klyx.app.icons.FormatIndentIncrease
import com.klyx.app.icons.FormatListNumbered
import com.klyx.app.icons.SpaceBar
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import kotlinx.collections.immutable.persistentListOf

@Composable
fun EditingSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit
) {
    val s = strings

    SettingsSubsection(strings.editingSection) {
        SwitchSettingItem(
            title = strings.pinLineNumbers,
            subtitle = strings.pinLineNumbersDesc,
            checked = settings.pinLineNumbers,
            onCheckedChange = {
                update { copy(pinLineNumbers = it) }
            },
            leadingIcon = { Icon(Icons.Rounded.FormatListNumbered, null) }
        )

        SwitchSettingItem(
            title = strings.deleteEmptyLinesFast,
            subtitle = strings.deleteEmptyLinesFastDesc,
            checked = settings.deleteEmptyLineFast,
            onCheckedChange = { update { copy(deleteEmptyLineFast = it) } },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Backspace, null) }
        )

        SelectorItem(
            label = strings.deleteMultipleSpaces,
            description = strings.deleteMultipleSpacesDesc,
            options = persistentListOf(-1, 1, 2, 4, 8),
            selected = settings.deleteMultiSpaces,
            optionLabel = { value ->
                when (value) {
                    -1 -> s.followTabSize
                    1 -> s.oneSpace
                    else -> s.nSpaces(value)
                }
            },
            onSelectionChanged = { update { copy(deleteMultiSpaces = it) } },
            leadingIcon = { Icon(Icons.Rounded.SpaceBar, null) }
        )

        SwitchSettingItem(
            title = strings.symbolPairAutoCompletion,
            subtitle = strings.symbolPairAutoCompletionDesc,
            checked = settings.symbolPairAutoCompletion,
            onCheckedChange = { update { copy(symbolPairAutoCompletion = it) } },
            leadingIcon = { Icon(Icons.Rounded.DataArray, null) }
        )

        SwitchSettingItem(
            title = strings.autoIndent,
            subtitle = strings.autoIndentDesc,
            checked = settings.autoIndent,
            onCheckedChange = { update { copy(autoIndent = it) } },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Rounded.FormatIndentIncrease,
                    null
                )
            }
        )

        SwitchSettingItem(
            title = strings.formatOnPaste,
            subtitle = strings.formatOnPasteDesc,
            checked = settings.formatPastedText,
            onCheckedChange = { update { copy(formatPastedText = it) } },
            leadingIcon = { Icon(Icons.Rounded.ContentPaste, null) }
        )
    }
}
