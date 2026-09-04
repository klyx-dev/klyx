package com.klyx.presentation.screen.settings.editor

import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.api.data.preferences.MouseMode
import com.klyx.app.icons.MenuOpen
import com.klyx.app.icons.Mouse
import com.klyx.app.icons.UnfoldMore
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import kotlinx.collections.immutable.persistentListOf
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun MouseScrollingSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localFastScroll: Float,
    onLocalFastScrollChange: (Float) -> Unit,
    localWheelFactor: Float,
    onLocalWheelFactorChange: (Float) -> Unit
) {
    val s = strings

    SettingsSubsection(strings.mouseScrollingSection) {
        SelectorItem(
            label = strings.mouseMode,
            description = strings.mouseModeDesc,
            options = persistentListOf(
                MouseMode.Auto,
                MouseMode.Always,
                MouseMode.Never
            ),
            selected = settings.mouseMode,
            optionLabel = {
                when (it) {
                    MouseMode.Auto -> s.mouseAuto
                    MouseMode.Always -> s.mouseAlways
                    MouseMode.Never -> s.mouseNever
                    else -> it.name
                }
            },
            optionDescription = { mode ->
                when (mode) {
                    MouseMode.Auto -> s.mouseModeAutoDesc
                    MouseMode.Always -> s.mouseModeAlwaysDesc
                    MouseMode.Never -> s.mouseModeNeverDesc
                    else -> null
                }
            },
            onSelectionChanged = { update { copy(mouseMode = it) } },
            leadingIcon = { Icon(Icons.Rounded.Mouse, null) }
        )

        SwitchSettingItem(
            title = strings.alwaysShowScrollbars,
            subtitle = strings.alwaysShowScrollbarsDesc,
            checked = settings.mouseModeAlwaysShowScrollbars,
            onCheckedChange = { update { copy(mouseModeAlwaysShowScrollbars = it) } },
            leadingIcon = { Icon(Icons.Rounded.UnfoldMore, null) }
        )

        SwitchSettingItem(
            title = strings.mouseContextMenu,
            subtitle = strings.mouseContextMenuDesc,
            checked = settings.mouseContextMenu,
            onCheckedChange = { update { copy(mouseContextMenu = it) } },
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.MenuOpen, null) }
        )

        SliderSettingsItem(
            label = strings.fastScrollSensitivity,
            value = localFastScroll,
            onValueChange = onLocalFastScrollChange,
            valueRange = 1f..10f,
            steps = 8,
            onValueChangeFinished = {
                if (localFastScroll != settings.fastScrollSensitivity) {
                    update { copy(fastScrollSensitivity = localFastScroll) }
                }
            },
            valueText = { "${it.roundToInt()}x" }
        )

        SliderSettingsItem(
            label = strings.mouseWheelFactor,
            value = localWheelFactor,
            onValueChange = onLocalWheelFactorChange,
            valueRange = 0.5f..5f,
            steps = 8,
            onValueChangeFinished = {
                if (localWheelFactor != settings.mouseWheelScrollFactor) {
                    update { copy(mouseWheelScrollFactor = localWheelFactor) }
                }
            },
            valueText = { String.format(Locale.ROOT, "%.1fx", it) }
        )
    }
}
