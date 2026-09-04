package com.klyx.presentation.screen.settings.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import com.klyx.api.data.preferences.EditorSettings
import com.klyx.app.icons.FilterCenterFocus
import com.klyx.app.icons.PushPin
import com.klyx.app.icons.UnfoldLess
import com.klyx.i18n.strings
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SliderSettingsItem
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import kotlin.math.roundToInt

@Composable
fun StickyScrollSettingsSection(
    settings: EditorSettings,
    update: (suspend EditorSettings.() -> EditorSettings) -> Unit,
    localStickyMax: Float,
    onLocalStickyMaxChange: (Float) -> Unit
) {
    SettingsSubsection(strings.stickyScrollSection) {
        SwitchSettingItem(
            title = strings.enableStickyScroll,
            subtitle = strings.enableStickyScrollDesc,
            checked = settings.stickyScroll,
            onCheckedChange = { update { copy(stickyScroll = it) } },
            leadingIcon = { Icon(Icons.Rounded.PushPin, null) }
        )

        AnimatedVisibility(
            visible = settings.stickyScroll,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = 400f
                )
            ) + fadeIn(animationSpec = spring(stiffness = 400f)),
            exit = shrinkVertically(animationSpec = spring(stiffness = 500f)) + fadeOut(
                animationSpec = spring(stiffness = 500f)
            )
        ) {
            SliderSettingsItem(
                label = strings.maxStickyLines,
                value = localStickyMax,
                onValueChange = onLocalStickyMaxChange,
                valueRange = 1f..10f,
                steps = 8,
                onValueChangeFinished = {
                    if (localStickyMax.roundToInt() != settings.stickyScrollMaxLines) {
                        update { copy(stickyScrollMaxLines = localStickyMax.roundToInt()) }
                    }
                },
                valueText = { "${it.roundToInt()}" }
            )
        }

        SwitchSettingItem(
            title = strings.preferInnerScope,
            subtitle = strings.preferInnerScopeDesc,
            checked = settings.stickyScrollPreferInnerScope,
            onCheckedChange = { update { copy(stickyScrollPreferInnerScope = it) } },
            leadingIcon = { Icon(Icons.Rounded.FilterCenterFocus, null) }
        )

        SwitchSettingItem(
            title = strings.autoCollapse,
            subtitle = strings.autoCollapseDesc,
            checked = settings.stickyScrollAutoCollapse,
            onCheckedChange = { update { copy(stickyScrollAutoCollapse = it) } },
            leadingIcon = { Icon(Icons.Rounded.UnfoldLess, null) }
        )
    }
}
