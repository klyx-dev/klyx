package com.klyx

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.klyx.core.event.EventBus
import com.klyx.core.notification.ui.NotificationOverlay
import com.klyx.core.platform.Os
import com.klyx.core.platform.currentOs
import com.klyx.core.platform.currentPlatform
import com.klyx.di.LocalKlyxViewModel
import com.klyx.ui.DisclaimerDialog
import com.klyx.ui.component.PermissionDialog
import com.klyx.ui.event.TerminalNotificationTapEvent
import com.klyx.ui.page.SettingsPage
import com.klyx.ui.page.main.MainPage
import com.klyx.ui.page.settings.about.AboutPage
import com.klyx.ui.page.settings.appearance.AppearancePreferences
import com.klyx.ui.page.settings.appearance.DarkThemePreferences
import com.klyx.ui.page.settings.editor.EditorPreferences
import com.klyx.ui.page.settings.general.GeneralPreferences
import com.klyx.ui.page.terminal.TerminalPage

@Composable
fun MainScreen() {
    val klyxViewModel = LocalKlyxViewModel.current
    val appState by klyxViewModel.appState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val navigationState = rememberNavigationState(startRoute = Route.Main)
        val navigator = remember { Navigator(navigationState) }

        LaunchedEffect(Unit) {
            EventBus.INSTANCE.subscribe<TerminalNotificationTapEvent> {
                if (navigator.currentTopLevelRoute != Route.Terminal) {
                    navigator.navigateTo(Route.Terminal)
                }
            }
        }

        val entryProvider = entryProvider {
            entry<Route.Main> {
                MainPage(modifier = Modifier.fillMaxSize())
            }

            entry<Route.Terminal> {
                Surface {
                    if (currentOs() == Os.Android) {
                        TerminalPage(modifier = Modifier.fillMaxSize())
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Terminal not supported on ${currentPlatform()} platform")
                        }
                    }
                }
            }

            entry<Route.Settings> { SettingsPage() }

            settingsScreenEntries()
        }

        CompositionLocalProvider(LocalNavigator provides navigator) {
            NavDisplay(
                modifier = Modifier.align(Alignment.Center),
                entries = navigationState.toEntries(entryProvider),
                onBack = { navigator.navigateBack() },
                transitionSpec = {
                    // Slide in from right when navigating forward
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(Navigator.ANIMATION_DURATION)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(Navigator.ANIMATION_DURATION)
                    )
                },
                popTransitionSpec = {
                    // Slide in from left when navigating back
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(Navigator.ANIMATION_DURATION)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(Navigator.ANIMATION_DURATION)
                    )
                },
                predictivePopTransitionSpec = {
                    // Slide in from left when navigating back
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = tween(Navigator.ANIMATION_DURATION)
                    ) togetherWith slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(Navigator.ANIMATION_DURATION)
                    )
                }
            )
        }

        if (appState.showPermissionDialog) {
            PermissionDialog(
                onDismissRequest = { klyxViewModel.dismissPermissionDialog() },
                onRequestPermission = { requestFileAccessPermission() }
            )
        }

        val disclaimerAccepted by DisclaimerManager.accepted.collectAsState()

        if (disclaimerAccepted == false) {
            DisclaimerDialog(onAccept = DisclaimerManager::accept)
        }

        NotificationOverlay()
    }
}

private fun EntryProviderScope<NavKey>.settingsScreenEntries() {
    entry<SettingsRoute.General> { GeneralPreferences() }
    entry<SettingsRoute.Appearance> { AppearancePreferences() }
    entry<SettingsRoute.DarkTheme> { DarkThemePreferences() }
    entry<SettingsRoute.Editor> { EditorPreferences() }
    entry<SettingsRoute.About> { AboutPage() }
}

