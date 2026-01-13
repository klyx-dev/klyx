package com.klyx.ui.page.settings.appearance

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.klyx.LocalNavigator
import com.klyx.core.settings.LocalAppSettings
import com.klyx.core.settings.update
import com.klyx.core.theme.Appearance
import com.klyx.core.theme.Contrast
import com.klyx.core.theme.LocalContrast
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.PreferenceSingleChoiceItem
import com.klyx.core.ui.component.PreferenceSubtitle
import com.klyx.core.ui.component.PreferenceSwitchVariant
import com.klyx.icons.Contrast
import com.klyx.icons.Icons
import com.klyx.resources.Res
import com.klyx.resources.additional_settings
import com.klyx.resources.dark_theme
import com.klyx.resources.follow_system
import com.klyx.resources.high_contrast
import com.klyx.resources.off
import com.klyx.resources.on
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkThemePreferences() {
    val navigator = LocalNavigator.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appSettings = LocalAppSettings.current
    val isDarkMode = LocalIsDarkMode.current
    val isHighContrastModeEnabled = LocalContrast.current == Contrast.High

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(Res.string.dark_theme))
                },
                navigationIcon = { BackButton(navigator::navigateBack) },
                scrollBehavior = scrollBehavior,
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier, contentPadding = paddingValues) {
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
                    icon = Icons.Contrast,
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
}
