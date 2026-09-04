package com.klyx.presentation.screen.settings.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.app.icons.FormatLineSpacing
import com.klyx.data.preferences.updateEditorSettings
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SegmentedSettingsItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun CommonSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localFontSize: Float,
    onLocalFontSizeChange: (Float) -> Unit,
    localFontFamily: FontFamily,
    customFontUri: String?,
    scope: CoroutineScope
) {
    val s = strings

    SettingsSubsection(strings.commonSection) {
        FontFamilySettingItem(
            currentFontFamily = localFontFamily,
            customFontUri = customFontUri,
            onClearCustomFont = {
                scope.launch {
                    updateEditorSettings { copy(customFontUri = null) }
                }
            },
            onCustomFontPicked = { uriString ->
                scope.launch {
                    updateEditorSettings { copy(customFontUri = uriString) }
                }
            }
        )

        SliderSettingsItem(
            label = strings.fontSize,
            value = localFontSize,
            onValueChange = onLocalFontSizeChange,
            valueRange = 10f..32f,
            steps = 21,
            onValueChangeFinished = {
                if (localFontSize != settings.fontSize) {
                    update { copy(fontSize = localFontSize) }
                }
            },
            valueText = { "${it.roundToInt()}sp" }
        )

        SegmentedSettingsItem(
            label = strings.tabSize,
            options = persistentListOf(2, 4, 8),
            currentValue = settings.tabSize,
            onValueChange = { update { copy(tabSize = it) } },
            valueText = { s.nSpaces(it) }
        )

        SwitchSettingItem(
            title = strings.wordWrap,
            subtitle = strings.wordWrapDesc,
            checked = settings.wordWrap,
            onCheckedChange = { update { copy(wordWrap = it) } },
            leadingIcon = { Icon(Icons.Rounded.FormatLineSpacing, null) }
        )
    }
}
