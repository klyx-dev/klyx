package com.klyx.presentation.screen.settings.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import com.klyx.R
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.app.icons.KeyboardTab
import com.klyx.app.icons.TouchApp
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SwitchSettingItem

@Composable
fun KeyboardInputSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit
) {
    SettingsSubsection(strings.keyboardInputSection) {
        SwitchSettingItem(
            title = strings.disableKeyboardSuggestions,
            subtitle = strings.disableKeyboardSuggestionsDesc,
            checked = settings.disallowSuggestions,
            onCheckedChange = { update { copy(disallowSuggestions = it) } },
            leadingIcon = {
                Icon(
                    painterResource(R.drawable.keyboard_off_24px),
                    null
                )
            }
        )

        SwitchSettingItem(
            title = strings.enhancedHomeAndEnd,
            subtitle = strings.enhancedHomeAndEndDesc,
            checked = settings.enhancedHomeAndEnd,
            onCheckedChange = { update { copy(enhancedHomeAndEnd = it) } },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.KeyboardTab, null) }
        )

        SwitchSettingItem(
            title = strings.reselectOnLongPress,
            subtitle = strings.reselectOnLongPressDesc,
            checked = settings.reselectOnLongPress,
            onCheckedChange = { update { copy(reselectOnLongPress = it) } },
            leadingIcon = { Icon(Icons.Rounded.TouchApp, null) }
        )
    }
}
