package com.klyx.presentation.screen.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.TextFormat
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
import com.klyx.data.preferences.LocalAppSettings
import com.klyx.data.preferences.updateTerminalSettings
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.settings.components.SelectorItem
import com.klyx.presentation.screen.settings.components.SettingsItem
import com.klyx.presentation.screen.settings.components.SettingsSubsection
import com.klyx.presentation.screen.settings.components.SwitchSettingItem
import com.klyx.terminal.emulator.CursorStyle
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalSettings() {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val settings = LocalAppSettings.current.terminal

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Terminal") },
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
                SettingsSubsection(title = "Session") {

                    SettingsItem(
                        title = "Current User",
                        subtitle = settings.currentUser ?: "Not initialized",
                        onClick = {},
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.person_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )

                    SwitchSettingItem(
                        title = "Open as Root",
                        subtitle = "Start terminal sessions as the root user",
                        checked = settings.openAsRoot,
                        onCheckedChange = { isChecked ->
                            scope.launch {
                                updateTerminalSettings { copy(openAsRoot = isChecked) }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                painterResource(R.drawable.admin_panel_settings_24px),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                }
            }

            item {
                SettingsSubsection(title = "Emulator") {
                    SelectorItem(
                        label = "Cursor Style",
                        description = "Choose the shape of the terminal cursor.",
                        options = CursorStyle.availableStyles().toImmutableList(),
                        selected = settings.cursorStyle,
                        optionLabel = { style ->
                            when (style) {
                                CursorStyle.Block -> "Block"
                                CursorStyle.Underline -> "Underline"
                                CursorStyle.Bar -> "Bar"
                                else -> "Unknown"
                            }
                        },
                        optionDescription = { style ->
                            when (style) {
                                CursorStyle.Block -> "A solid rectangle after the character"
                                CursorStyle.Underline -> "A horizontal line below the character"
                                CursorStyle.Bar -> "A thin vertical line after the character"
                                else -> null
                            }
                        },
                        onSelectionChanged = { selectedStyle ->
                            scope.launch {
                                updateTerminalSettings { copy(cursorStyle = selectedStyle) }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.TextFormat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    )
                }
            }

        }
    }
}
