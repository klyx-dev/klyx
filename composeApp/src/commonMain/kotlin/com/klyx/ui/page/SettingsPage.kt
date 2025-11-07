package com.klyx.ui.page

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.klyx.AppRoute
import com.klyx.LocalNavigator
import com.klyx.LocalWindowAdaptiveInfo
import com.klyx.Navigator
import com.klyx.ProvideNavigator
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
import com.klyx.strings
import com.klyx.title
import com.klyx.ui.page.settings.about.AboutPage
import com.klyx.ui.page.settings.appearance.AppearancePreferences
import com.klyx.ui.page.settings.appearance.DarkThemePreferences
import com.klyx.ui.page.settings.editor.EditorPreferences
import com.klyx.ui.page.settings.general.GeneralPreferences
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
fun SettingsPage(modifier: Modifier = Modifier.fillMaxSize()) {
    SettingsListDetailPane(modifier)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun SettingsListDetailPane(modifier: Modifier) {
    val navigator = LocalNavigator.current
    val paneNavigator = rememberListDetailPaneScaffoldNavigator<AppRoute>()
    val backNavigationBehavior = BackNavigationBehavior.PopUntilCurrentDestinationChange
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val onNavigateBack: () -> Unit = {
        coroutineScope.launch {
            with(navigator) {
                paneNavigator.navigateBackOrPopScreen(backNavigationBehavior)
            }
        }
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    AnimatedContent(
                        targetState = paneNavigator.currentDestination?.contentKey,
                        transitionSpec = { fadeIn().togetherWith(fadeOut()) }
                    ) { route ->
                        Text(text = route?.title ?: stringResource(strings.settings))
                    }
                },
                navigationIcon = { BackButton(onNavigateBack) },
                scrollBehavior = scrollBehavior,
                expandedHeight = TopAppBarDefaults.LargeAppBarExpandedHeight + 24.dp
            )
        }
    ) { padding ->

        val onNavigateTo: (AppRoute) -> Unit = { route ->
            coroutineScope.launch {
                paneNavigator.navigateTo(
                    pane = ListDetailPaneScaffoldRole.Detail,
                    contentKey = route
                )
            }
        }

        ProvideNavigator(onNavigateTo, onNavigateBack) {
            ListDetailPaneScaffold(
                modifier = Modifier.fillMaxSize().padding(padding),
                directive = calculatePaneScaffoldDirective(LocalWindowAdaptiveInfo.current),
                value = paneNavigator.scaffoldValue,
                listPane = {
                    AnimatedPane {
                        SettingsListPane(
                            modifier = Modifier.fillMaxSize(),
                            isSelected = { it == paneNavigator.currentDestination?.contentKey },
                            onSelect = { route ->
                                coroutineScope.launch {
                                    paneNavigator.navigateTo(
                                        pane = ListDetailPaneScaffoldRole.Detail,
                                        contentKey = route
                                    )
                                }
                            }
                        )
                    }
                },
                detailPane = {
                    AnimatedPane {
                        SettingsDetailPane(paneNavigator.currentDestination?.contentKey)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
context(navigator: Navigator)
private suspend fun <T> ThreePaneScaffoldNavigator<T>.navigateBackOrPopScreen(
    backNavigationBehavior: BackNavigationBehavior
): Boolean {
    return if (canNavigateBack(backNavigationBehavior)) {
        navigateBack(backNavigationBehavior)
    } else {
        navigator.navigateBack()
        false
    }
}

@Composable
private fun SettingsListPane(
    onSelect: (AppRoute) -> Unit,
    isSelected: (AppRoute) -> Boolean,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val backgroundColor = @Composable { route: AppRoute ->
        if (isSelected(route)) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surface
        }
    }

    val iconColor = @Composable { route: AppRoute ->
        if (isSelected(route)) {
            contentColorFor(backgroundColor(route))
        } else {
            Color.Unspecified
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding
    ) {
        item {
            SettingItem(
                title = stringResource(Res.string.general_settings),
                description = stringResource(Res.string.general_settings_desc),
                icon = Icons.Outlined.Settings,
                backgroundColor = backgroundColor(AppRoute.Settings.GeneralPreferences),
                iconColor = iconColor(AppRoute.Settings.GeneralPreferences),
                onClick = { onSelect(AppRoute.Settings.GeneralPreferences) }
            )
        }

        item {
            SettingItem(
                title = stringResource(Res.string.editor_settings),
                description = stringResource(Res.string.editor_settings_desc),
                icon = Icons.Outlined.Code,
                backgroundColor = backgroundColor(AppRoute.Settings.EditorPreferences),
                iconColor = iconColor(AppRoute.Settings.EditorPreferences),
                onClick = { onSelect(AppRoute.Settings.EditorPreferences) }
            )
        }

        item {
            SettingItem(
                title = stringResource(Res.string.look_and_feel),
                description = stringResource(Res.string.display_settings),
                icon = Icons.Outlined.Palette,
                backgroundColor = backgroundColor(AppRoute.Settings.Appearance),
                iconColor = iconColor(AppRoute.Settings.Appearance),
                onClick = { onSelect(AppRoute.Settings.Appearance) },
            )
        }

        item {
            SettingItem(
                title = stringResource(Res.string.about),
                description = stringResource(Res.string.about_page),
                icon = Icons.Outlined.Info,
                backgroundColor = backgroundColor(AppRoute.Settings.About),
                iconColor = iconColor(AppRoute.Settings.About),
                onClick = { onSelect(AppRoute.Settings.About) },
            )
        }
    }
}

@Composable
private fun SettingsDetailPane(route: AppRoute?) {
    when (route) {
        AppRoute.Settings.About -> AboutPage()
        AppRoute.Settings.Appearance -> AppearancePreferences()
        AppRoute.Settings.EditorPreferences -> EditorPreferences()
        AppRoute.Settings.GeneralPreferences -> GeneralPreferences()
        AppRoute.Settings.DarkTheme -> DarkThemePreferences()
        else -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            Text("Select a setting")
        }
    }
}
