package com.klyx.presentation.screen.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.klyx.R
import com.klyx.api.data.preferences.AppLanguage
import com.klyx.api.data.preferences.AppTheme
import com.klyx.api.data.preferences.AppearanceSettings
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.app.icons.Animation
import com.klyx.app.icons.Contrast
import com.klyx.app.icons.Fullscreen
import com.klyx.app.icons.Language
import com.klyx.app.icons.LightMode
import com.klyx.data.preferences.updateAppearanceSettings
import com.klyx.i18n.strings
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettings() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val settings = LocalAppSettings.current.appearance

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(strings.appearance) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                        onClick = { navigator.navigateBack() },
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = strings.back
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 16.dp
            )
        ) {
            item {
                ApplicationThemeSection(settings = settings, scope = scope)
            }

            item {
                WindowMotionSection(settings = settings, scope = scope)
            }
        }
    }
}

@Composable
private fun ApplicationThemeSection(
    settings: AppearanceSettings,
    scope: CoroutineScope
) {
    val s = strings

    SettingsSubsection(title = strings.applicationTheme) {
        SelectorItem(
            label = strings.appTheme,
            description = strings.appThemeDesc,
            options = AppTheme.entries.toImmutableList(),
            selected = settings.theme,
            optionLabel = {
                when (it) {
                    AppTheme.System -> s.themeFollowSystem
                    AppTheme.Light -> s.themeLight
                    AppTheme.Dark -> s.themeDark
                }
            },
            onSelectionChanged = {
                scope.launch {
                    updateAppearanceSettings { copy(theme = it) }
                }
            },
            leadingIcon = {
                Icon(
                    Icons.Outlined.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )

        SelectorItem(
            label = strings.language,
            description = strings.languageDesc,
            options = AppLanguage.entries.toImmutableList(),
            selected = settings.language,
            optionLabel = AppLanguage::displayName,
            onSelectionChanged = {
                scope.launch {
                    updateAppearanceSettings { copy(language = it) }
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        )

        SwitchSettingItem(
            title = strings.amoledDarkMode,
            subtitle = strings.amoledDarkModeDesc,
            checked = settings.amoledDarkMode,
            enabled = LocalIsDarkMode.current,
            onCheckedChange = { isChecked ->
                scope.launch {
                    updateAppearanceSettings { copy(amoledDarkMode = isChecked) }
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Rounded.Contrast,
                    contentDescription = null,
                    tint = if (!LocalIsDarkMode.current) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        )
    }
}

@Composable
private fun WindowMotionSection(
    settings: AppearanceSettings,
    scope: CoroutineScope
) {
    SettingsSubsection(strings.windowAndMotion) {
        SwitchSettingItem(
            title = strings.immersiveMode,
            subtitle = strings.immersiveModeDesc,
            checked = settings.immersiveMode,
            onCheckedChange = { isChecked ->
                scope.launch {
                    updateAppearanceSettings { copy(immersiveMode = isChecked) }
                }
            },
            leadingIcon = { Icon(Icons.Rounded.Fullscreen, null) }
        )

        SwitchSettingItem(
            title = strings.terminalInTopbar,
            subtitle = strings.terminalInTopbarDesc,
            checked = settings.showTerminalInTopbar,
            onCheckedChange = { isChecked ->
                scope.launch {
                    updateAppearanceSettings { copy(showTerminalInTopbar = isChecked) }
                }
            },
            leadingIcon = { Icon(painterResource(R.drawable.terminal_2_24px), null) }
        )

        SwitchSettingItem(
            title = strings.reduceMotion,
            subtitle = strings.reduceMotionDesc,
            checked = settings.reduceMotion,
            onCheckedChange = { isChecked ->
                scope.launch {
                    updateAppearanceSettings { copy(reduceMotion = isChecked) }
                }
            },
            leadingIcon = { Icon(Icons.Rounded.Animation, null) }
        )
    }
}
