package com.klyx.ui.page.settings.appearance

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.klyx.core.LocalAppSettings
import com.klyx.core.settings.update
import com.klyx.core.theme.Appearance
import com.klyx.core.theme.Contrast
import com.klyx.core.theme.LocalContrast
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.PreferenceSingleChoiceItem
import com.klyx.core.ui.component.PreferenceSubtitle
import com.klyx.core.ui.component.PreferenceSwitchVariant
import com.klyx.res.Res
import com.klyx.res.additional_settings
import com.klyx.res.dark_theme
import com.klyx.res.follow_system
import com.klyx.res.high_contrast
import com.klyx.res.off
import com.klyx.res.on
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DarkThemePreferences(onNavigateBack: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appSettings = LocalAppSettings.current
    val isHighContrastModeEnabled = LocalContrast.current == Contrast.High

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(modifier = Modifier, text = stringResource(Res.string.dark_theme))
                },
                navigationIcon = { BackButton { onNavigateBack() } },
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
                    icon = Icons.Outlined.Contrast,
                    isChecked = isHighContrastModeEnabled,
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
