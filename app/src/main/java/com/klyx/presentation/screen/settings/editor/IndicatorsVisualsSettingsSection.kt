package com.klyx.presentation.screen.settings.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.app.icons.DataObject
import com.klyx.app.icons.FormatBold
import com.klyx.app.icons.RoundedCorner
import com.klyx.app.icons.TextFormat
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun IndicatorsVisualsSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localWaveLength: Float,
    onLocalWaveLengthChange: (Float) -> Unit,
    localWaveWidth: Float,
    onLocalWaveWidthChange: (Float) -> Unit,
    localWaveAmplitude: Float,
    onLocalWaveAmplitudeChange: (Float) -> Unit
) {
    SettingsSubsection(strings.indicatorsVisualsSection) {
        SwitchSettingItem(
            title = strings.roundTextBackground,
            subtitle = strings.roundTextBackgroundDesc,
            checked = settings.enableRoundTextBackground,
            onCheckedChange = { update { copy(enableRoundTextBackground = it) } },
            leadingIcon = { Icon(Icons.Rounded.RoundedCorner, null) }
        )

        SwitchSettingItem(
            title = strings.highlightMatchingDelimiters,
            subtitle = strings.highlightMatchingDelimitersDesc,
            checked = settings.highlightMatchingDelimiters,
            onCheckedChange = { update { copy(highlightMatchingDelimiters = it) } },
            leadingIcon = { Icon(Icons.Rounded.DataObject, null) }
        )

        SwitchSettingItem(
            title = strings.boldMatchingDelimiters,
            subtitle = strings.boldMatchingDelimitersDesc,
            checked = settings.boldMatchingDelimiters,
            onCheckedChange = { update { copy(boldMatchingDelimiters = it) } },
            leadingIcon = { Icon(Icons.Rounded.FormatBold, null) }
        )

        SwitchSettingItem(
            title = strings.inlayHints,
            subtitle = strings.inlayHintsDesc,
            checked = settings.inlayHints,
            onCheckedChange = { update { copy(inlayHints = it) } },
            leadingIcon = { Icon(Icons.Rounded.TextFormat, null) }
        )

        SliderSettingsItem(
            label = strings.errorWaveLength,
            value = localWaveLength,
            onValueChange = onLocalWaveLengthChange,
            valueRange = 5f..30f,
            steps = 24,
            onValueChangeFinished = {
                if (localWaveLength != settings.indicatorWaveLength) {
                    update { copy(indicatorWaveLength = localWaveLength) }
                }
            },
            valueText = { "${it.roundToInt()}dp" }
        )

        SliderSettingsItem(
            label = strings.errorWaveWidth,
            value = localWaveWidth,
            onValueChange = onLocalWaveWidthChange,
            valueRange = 0.5f..5f,
            steps = 8,
            onValueChangeFinished = {
                if (localWaveWidth != settings.indicatorWaveWidth) {
                    update { copy(indicatorWaveWidth = localWaveWidth) }
                }
            },
            valueText = { String.format(Locale.ROOT, "%.1f", it) }
        )

        SliderSettingsItem(
            label = strings.errorWaveAmplitude,
            value = localWaveAmplitude,
            onValueChange = onLocalWaveAmplitudeChange,
            valueRange = 1f..10f,
            steps = 8,
            onValueChangeFinished = {
                if (localWaveAmplitude != settings.indicatorWaveAmplitude) {
                    update { copy(indicatorWaveAmplitude = localWaveAmplitude) }
                }
            },
            valueText = { "${it.roundToInt()}dp" }
        )
    }
}
