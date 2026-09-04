package com.klyx.presentation.screen.settings.terminal

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.klyx.api.data.preferences.TerminalSettings as TerminalSettingsPrefs
import com.klyx.data.preferences.updateTerminalSettings
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SessionSettingsSection(
    settings: TerminalSettingsPrefs,
    scope: CoroutineScope
) {
    SettingsSubsection(title = strings.sessionSection) {
        SwitchSettingItem(
            title = strings.showMotd,
            subtitle = strings.showMotdDesc,
            checked = settings.showMotd,
            onCheckedChange = { showMotd ->
                scope.launch {
                    updateTerminalSettings { copy(showMotd = showMotd) }
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )
    }
}
