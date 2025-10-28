package com.klyx.ui.page

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.klyx.AppRoute
import com.klyx.core.ui.component.BackButton
import com.klyx.core.ui.component.SettingItem
import com.klyx.res.Res
import com.klyx.res.about
import com.klyx.res.about_page
import com.klyx.res.display_settings
import com.klyx.res.editor_settings
import com.klyx.res.editor_settings_desc
import com.klyx.res.general_settings
import com.klyx.res.general_settings_desc
import com.klyx.res.look_and_feel
import com.klyx.res.settings
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onNavigateBack: () -> Unit,
    onNavigateTo: (Any) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(text = stringResource(Res.string.settings)) },
                navigationIcon = { BackButton(onNavigateBack) },
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
                    icon = Icons.Outlined.Settings
                ) {
                    onNavigateTo(AppRoute.Settings.GeneralPreferences)
                }
            }

            item {
                SettingItem(
                    title = stringResource(Res.string.editor_settings),
                    description = stringResource(Res.string.editor_settings_desc),
                    icon = Icons.Outlined.Code
                ) {
                    onNavigateTo(AppRoute.Settings.EditorPreferences)
                }
            }

            item {
                SettingItem(
                    title = stringResource(Res.string.look_and_feel),
                    description = stringResource(Res.string.display_settings),
                    icon = Icons.Outlined.Palette,
                ) {
                    onNavigateTo(AppRoute.Settings.Appearance)
                }
            }

            item {
                SettingItem(
                    title = stringResource(Res.string.about),
                    description = stringResource(Res.string.about_page),
                    icon = Icons.Outlined.Info,
                ) {
                    onNavigateTo(AppRoute.Settings.About)
                }
            }
        }
    }
}
