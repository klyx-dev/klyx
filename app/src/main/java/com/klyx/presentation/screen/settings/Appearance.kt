package com.klyx.presentation.screen.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Contrast
import androidx.compose.material.icons.rounded.Fullscreen
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
import androidx.compose.ui.unit.dp
import com.klyx.api.data.preferences.AppTheme
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.data.preferences.updateAppearanceSettings
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.SettingScreens
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import com.klyx.api.ui.theme.LocalIsDarkMode
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreens.Appearance() {
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
                title = { Text("Appearance") },
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
                            contentDescription = "Back"
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
                SettingsSubsection(title = "Application Theme") {
                    SelectorItem(
                        label = "App Theme",
                        description = "Switch between light, dark, or follow system appearance.",
                        options = AppTheme.entries.toImmutableList(),
                        selected = settings.theme,
                        optionLabel = AppTheme::displayName,
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

                    SwitchSettingItem(
                        title = "AMOLED Dark Mode",
                        subtitle = "Turn dark backgrounds pure black to save battery on OLED screens",
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

            item {
                SettingsSubsection("Window & Motion") {
                    SwitchSettingItem(
                        title = "Immersive Mode",
                        subtitle = "Hide the system status and navigation bars to maximize coding space",
                        checked = settings.immersiveMode,
                        onCheckedChange = { isChecked ->
                            scope.launch {
                                updateAppearanceSettings { copy(immersiveMode = isChecked) }
                            }
                        },
                        leadingIcon = { Icon(Icons.Rounded.Fullscreen, null) }
                    )

                    SwitchSettingItem(
                        title = "Reduce Motion",
                        subtitle = "Disable UI animations for instant menu and dialog transitions",
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
        }
    }
}
