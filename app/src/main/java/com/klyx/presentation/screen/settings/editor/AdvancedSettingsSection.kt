package com.klyx.presentation.screen.settings.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.app.icons.KeyboardReturn
import com.klyx.app.icons.TextFormat
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SwitchSettingItem

@Composable
fun AdvancedSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit
) {
    SettingsSubsection(strings.advancedSection) {
        SwitchSettingItem(
            title = strings.selectCompletionOnEnter,
            subtitle = strings.selectCompletionOnEnterDesc,
            checked = settings.selectCompletionItemOnEnterForSoftKbd,
            onCheckedChange = { update { copy(selectCompletionItemOnEnterForSoftKbd = it) } },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardReturn, null) }
        )

        SwitchSettingItem(
            title = strings.useIcuLibrary,
            subtitle = strings.useIcuLibraryDesc,
            checked = settings.useICULibToSelectWords,
            onCheckedChange = { update { copy(useICULibToSelectWords = it) } },
            leadingIcon = { Icon(Icons.Rounded.TextFormat, null) }
        )
    }
}
