package com.klyx.ui.page.settings.appearance

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import com.klyx.LocalNavigator
import com.klyx.core.settings.LocalAppSettings
import com.klyx.core.settings.update
import com.klyx.core.theme.Appearance
import com.klyx.core.theme.Contrast
import com.klyx.core.theme.LocalContrast
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.core.ui.component.PreferenceSingleChoiceItem
import com.klyx.core.ui.component.PreferenceSubtitle
import com.klyx.core.ui.component.PreferenceSwitchVariant
import com.klyx.res.Res
import com.klyx.res.additional_settings
import com.klyx.res.follow_system
import com.klyx.res.high_contrast
import com.klyx.res.off
import com.klyx.res.on
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DarkThemePreferences() {
    val appSettings = LocalAppSettings.current
    val isDarkMode = LocalIsDarkMode.current
    val isHighContrastModeEnabled = LocalContrast.current == Contrast.High

    val navigator = LocalNavigator.current
    @Suppress("DEPRECATION")
    BackHandler { navigator.navigateBack() }

    LazyColumn {
        item {
            PreferenceSingleChoiceItem(
                text = stringResource(Res.string.follow_system),
                selected = appSettings.appearance == Appearance.System,
            ) {
                appSettings.update { it.copy(appearance = Appearance.System) }
            }
        }

        item {
            PreferenceSingleChoiceItem(
                text = stringResource(Res.string.on),
                selected = appSettings.appearance == Appearance.Dark,
            ) {
                appSettings.update { it.copy(appearance = Appearance.Dark) }
            }
        }

        item {
            PreferenceSingleChoiceItem(
                text = stringResource(Res.string.off),
                selected = appSettings.appearance == Appearance.Light,
            ) {
                appSettings.update { it.copy(appearance = Appearance.Light) }
            }
        }

        item { PreferenceSubtitle(text = stringResource(Res.string.additional_settings)) }

        item {
            PreferenceSwitchVariant(
                title = stringResource(Res.string.high_contrast),
                icon = Icons.Outlined.Contrast,
                enabled = isDarkMode,
                isChecked = isHighContrastModeEnabled && isDarkMode,
                onClick = { checked ->
                    appSettings.update {
                        it.copy(contrast = if (checked) Contrast.High else Contrast.Normal)
                    }
                }
            )
        }
    }
}
