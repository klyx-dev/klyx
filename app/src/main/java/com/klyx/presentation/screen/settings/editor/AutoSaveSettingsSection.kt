package com.klyx.presentation.screen.settings.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.klyx.api.data.preferences.AutoSaveScope
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.app.icons.CopyAll
import com.klyx.app.icons.FolderOpen
import com.klyx.app.icons.Save
import com.klyx.app.icons.Storage
import com.klyx.app.icons.Update
import com.klyx.app.icons.VisibilityOff
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import kotlinx.collections.immutable.persistentListOf
import kotlin.math.roundToInt

@Composable
fun AutoSaveSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localDelay: Float,
    onLocalDelayChange: (Float) -> Unit,
    localThreshold: Float,
    onLocalThresholdChange: (Float) -> Unit,
    localInterval: Float,
    onLocalIntervalChange: (Float) -> Unit
) {
    val s = strings
    SettingsSubsection(strings.autoSaveSection) {
        SwitchSettingItem(
            title = strings.autoSaveEnabled,
            subtitle = strings.autoSaveEnabledDesc,
            checked = settings.autoSave.enabled,
            onCheckedChange = { update { copy(autoSave = autoSave.copy(enabled = it)) } },
            leadingIcon = { Icon(Icons.Outlined.Save, null) }
        )

        AnimatedVisibility(
            visible = settings.autoSave.enabled,
            enter = expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f)) + fadeIn(
                spring(
                    stiffness = 400f
                )
            ),
            exit = shrinkVertically(spring(stiffness = 500f)) + fadeOut(spring(stiffness = 500f))
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                SliderSettingsItem(
                    label = strings.autoSaveDelay,
                    value = localDelay,
                    onValueChange = onLocalDelayChange,
                    valueRange = 200f..10000f,
                    steps = 48,
                    onValueChangeFinished = {
                        val coerced = localDelay.roundToInt().toLong().coerceIn(200L, 10000L)
                        if (coerced != settings.autoSave.delayMillis) {
                            update { copy(autoSave = autoSave.copy(delayMillis = coerced)) }
                        }
                    },
                    valueText = { "${it.roundToInt()} ms" }
                )

                SwitchSettingItem(
                    title = strings.autoSaveOnTyping,
                    subtitle = strings.autoSaveOnTypingDesc,
                    checked = settings.autoSave.onTyping,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(onTyping = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.Info, null) }
                )

                SwitchSettingItem(
                    title = strings.autoSaveOnAppPause,
                    subtitle = strings.autoSaveOnAppPauseDesc,
                    checked = settings.autoSave.onAppPause,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(onAppPause = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.VisibilityOff, null) }
                )

                SwitchSettingItem(
                    title = strings.autoSaveOnTabSwitch,
                    subtitle = strings.autoSaveOnTabSwitchDesc,
                    checked = settings.autoSave.onTabSwitch,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(onTabSwitch = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.FolderOpen, null) }
                )

                SelectorItem(
                    label = strings.autoSaveScope,
                    description = strings.autoSaveScopeDesc,
                    options = persistentListOf(AutoSaveScope.ACTIVE_TAB, AutoSaveScope.ALL_TABS),
                    selected = settings.autoSave.scope,
                    optionLabel = {
                        when (it) {
                            ACTIVE_TAB -> s.autoSaveScopeActiveTab
                            ALL_TABS -> s.autoSaveScopeAllTabs
                        }
                    },
                    optionDescription = { scope ->
                        when (scope) {
                            ACTIVE_TAB -> s.autoSaveScopeActiveTabDesc
                            ALL_TABS -> s.autoSaveScopeAllTabsDesc
                        }
                    },
                    onSelectionChanged = { update { copy(autoSave = autoSave.copy(scope = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.CopyAll, null) }
                )

                SwitchSettingItem(
                    title = strings.autoSaveSkipLargeFiles,
                    subtitle = strings.autoSaveSkipLargeFilesDesc,
                    checked = settings.autoSave.skipLargeFiles,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(skipLargeFiles = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.Storage, null) }
                )

                AnimatedVisibility(
                    visible = settings.autoSave.skipLargeFiles,
                    enter = expandVertically(
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        )
                    ) + fadeIn(spring(stiffness = 400f)),
                    exit = shrinkVertically(spring(stiffness = 500f)) + fadeOut(spring(stiffness = 500f))
                ) {
                    SliderSettingsItem(
                        label = strings.autoSaveLargeFileThreshold,
                        value = localThreshold,
                        onValueChange = onLocalThresholdChange,
                        valueRange = 50f..2048f,
                        steps = 15,
                        onValueChangeFinished = {
                            val v = localThreshold.roundToInt().coerceIn(50, 5000)
                            if (v != settings.autoSave.largeFileThresholdKb) {
                                update { copy(autoSave = autoSave.copy(largeFileThresholdKb = v)) }
                            }
                        },
                        valueText = { "${it.roundToInt()} KB" }
                    )
                }

                val periodicEnabled = settings.autoSave.periodicIntervalMillis != null
                SwitchSettingItem(
                    title = strings.autoSavePeriodic,
                    subtitle = strings.autoSavePeriodicDesc,
                    checked = periodicEnabled,
                    onCheckedChange = { enabled ->
                        update {
                            copy(
                                autoSave = autoSave.copy(
                                    periodicIntervalMillis = if (enabled) {
                                        localInterval.roundToInt().toLong().coerceIn(5000L, 120000L)
                                    } else null
                                )
                            )
                        }
                    },
                    leadingIcon = { Icon(Icons.Rounded.Update, null) }
                )

                AnimatedVisibility(
                    visible = periodicEnabled,
                    enter = expandVertically(
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = 400f
                        )
                    ) + fadeIn(spring(stiffness = 400f)),
                    exit = shrinkVertically(spring(stiffness = 500f)) + fadeOut(spring(stiffness = 500f))
                ) {
                    SliderSettingsItem(
                        label = strings.autoSaveInterval,
                        value = localInterval,
                        onValueChange = onLocalIntervalChange,
                        valueRange = 5000f..120000f,
                        steps = 22,
                        onValueChangeFinished = {
                            val v = localInterval.roundToInt().toLong().coerceIn(5000L, 120000L)
                            if (v != settings.autoSave.periodicIntervalMillis) {
                                update { copy(autoSave = autoSave.copy(periodicIntervalMillis = v)) }
                            }
                        },
                        valueText = { "${(it / 1000).roundToInt()} sec" }
                    )
                }

                SwitchSettingItem(
                    title = strings.autoSaveShowToast,
                    subtitle = strings.autoSaveShowToastDesc,
                    checked = settings.autoSave.showToast,
                    onCheckedChange = { update { copy(autoSave = autoSave.copy(showToast = it)) } },
                    leadingIcon = { Icon(Icons.Rounded.Info, null) }
                )

                Text(
                    text = strings.autoSavePerformanceNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
