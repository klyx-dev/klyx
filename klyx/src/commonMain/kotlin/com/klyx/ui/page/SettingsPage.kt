package com.klyx.ui.page

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
import androidx.compose.ui.unit.dp
import com.klyx.LocalNavigator
import com.klyx.SettingsRoute
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.SettingItem
import com.klyx.icons.Code
import com.klyx.icons.Icons
import com.klyx.icons.Info
import com.klyx.icons.Palette
import com.klyx.icons.Settings
import com.klyx.resources.Res
import com.klyx.resources.about
import com.klyx.resources.about_page
import com.klyx.resources.display_settings
import com.klyx.resources.editor_settings
import com.klyx.resources.editor_settings_desc
import com.klyx.resources.general_settings
import com.klyx.resources.general_settings_desc
import com.klyx.resources.look_and_feel
import com.klyx.resources.settings
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(modifier: Modifier = Modifier.fillMaxSize()) {
    val navigator = LocalNavigator.current

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.settings)) },
                navigationIcon = { BackButton(navigator::navigateBack) },
                scrollBehavior = scrollBehavior,
                expandedHeight = TopAppBarDefaults.LargeAppBarExpandedHeight + 24.dp
            )
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            item {
                SettingItem(
                    title = stringResource(Res.string.general_settings),
                    description = stringResource(Res.string.general_settings_desc),
                    icon = Icons.Settings
                ) {
                    navigator.navigateTo(SettingsRoute.General)
                }
            }

            item {
                SettingItem(
                    title = stringResource(Res.string.editor_settings),
                    description = stringResource(Res.string.editor_settings_desc),
                    icon = Icons.Code
                ) {
                    navigator.navigateTo(SettingsRoute.Editor)
                }
            }

            item {
                SettingItem(
                    title = stringResource(Res.string.look_and_feel),
                    description = stringResource(Res.string.display_settings),
                    icon = Icons.Palette,
                ) {
                    navigator.navigateTo(SettingsRoute.Appearance)
                }
            }

            item {
                SettingItem(
                    title = stringResource(Res.string.about),
                    description = stringResource(Res.string.about_page),
                    icon = Icons.Info,
                ) {
                    navigator.navigateTo(SettingsRoute.About)
                }
            }
        }
    }
}
