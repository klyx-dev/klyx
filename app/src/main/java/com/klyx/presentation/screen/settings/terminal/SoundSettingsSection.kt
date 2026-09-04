package com.klyx.presentation.screen.settings.terminal

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.klyx.api.data.preferences.TerminalSettings as TerminalSettingsPrefs
import com.klyx.api.util.sliderSteps
import com.klyx.app.icons.NotificationsOff
import com.klyx.app.icons.VolumeUp
import com.klyx.data.preferences.updateTerminalSettings
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import com.klyx.terminal.BellSoundType
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SoundSettingsSection(
    settings: TerminalSettingsPrefs,
    scope: CoroutineScope
) {
    SettingsSubsection(title = strings.sound) {
        SwitchSettingItem(
            title = strings.bellSound,
            subtitle = strings.bellSoundDesc,
            checked = settings.bellEnabled,
            onCheckedChange = { enabled ->
                scope.launch {
                    updateTerminalSettings { copy(bellEnabled = enabled) }
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = if (settings.bellEnabled) Icons.Rounded.Notifications
                    else Icons.Rounded.NotificationsOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )

        AnimatedVisibility(
            visible = settings.bellEnabled,
            enter = expandVertically(
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)
            ) + fadeIn(animationSpec = spring(stiffness = 400f)),
            exit = shrinkVertically(animationSpec = spring(stiffness = 500f)) + fadeOut(
                animationSpec = spring(stiffness = 500f)
            )
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SliderSettingsItem(
                    label = strings.bellVolume,
                    value = settings.bellVolume,
                    valueRange = 0f..1f,
                    steps = (0f..1f).sliderSteps(increment = 0.1f),
                    onValueChange = { volume ->
                        scope.launch {
                            updateTerminalSettings { copy(bellVolume = volume) }
                        }
                    },
                    valueText = { "${(it * 100).toInt()}%" }
                )

                val s = strings
                SelectorItem(
                    label = strings.bellSoundType,
                    description = strings.bellSoundTypeDesc,
                    options = BellSoundType.entries.toImmutableList(),
                    selected = settings.bellSoundType,
                    optionLabel = { type ->
                        when (type) {
                            Gentle -> s.bellGentle
                            BellSoundType.System -> s.bellSystem
                            VisualOnly -> s.bellVisualOnly
                        }
                    },
                    optionDescription = { type ->
                        when (type) {
                            Gentle -> s.bellGentleDesc
                            BellSoundType.System -> s.bellSystemDesc
                            VisualOnly -> s.bellVisualOnlyDesc
                        }
                    },
                    onSelectionChanged = { selectedType ->
                        scope.launch {
                            updateTerminalSettings { copy(bellSoundType = selectedType) }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                )
            }
        }
    }
}
