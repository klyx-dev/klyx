package com.klyx

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.composeunstyled.theme.Theme
import com.klyx.core.action.action
import com.klyx.document.DocumentPersistence
import com.klyx.document.UnsavedChangesDialog
import com.klyx.navigation.LocalNavigator
import com.klyx.navigation.Settings
import com.klyx.runtime.action.SaveActiveDocument
import com.klyx.runtime.action.SaveAllDocuments
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.settings.resolveActiveThemeName
import com.klyx.ui.provider.ProvideCompositionLocals
import com.klyx.ui.screen.HomeScreen
import com.klyx.ui.screen.settings.SettingsScreen
import com.klyx.ui.theme.KlyxTheme
import com.klyx.ui.theme.LocalColorScheme
import com.klyx.ui.theme.LocalCornerRadius
import com.klyx.ui.theme.background
import com.klyx.ui.theme.colors
import com.klyx.ui.utils.BackHandler
import org.koin.compose.koinInject

@Composable
fun Screen(onExit: () -> Unit = {}) {
    val settingsStore: SettingsStore = koinInject()
    val persistence: DocumentPersistence = koinInject()

    val settings by settingsStore.settings.collectAsState()

    val effectiveCornerRadius = if (settings.roundedUi) settings.cornerRadius.dp else 0.dp

    val resolvedThemeName = resolveActiveThemeName(
        themeMode = settings.themeMode,
        darkThemeName = settings.themeName,
        lightThemeName = settings.lightThemeName
    )

    ProvideCompositionLocals(
        themeName = resolvedThemeName,
        fontName = settings.fontName
    ) {
        CompositionLocalProvider(
            LocalCornerRadius provides effectiveCornerRadius,
        ) {
            KlyxTheme(colorScheme = LocalColorScheme.current) {
                val navigator = LocalNavigator.current
                val showSettings = navigator.currentRoute is Settings

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onPreviewKeyEvent { event ->
                            if (event.key == Key.S && (event.isCtrlPressed || event.isMetaPressed)) {
                                if (event.type == KeyEventType.KeyDown) {
                                    action(if (event.isShiftPressed) SaveAllDocuments else SaveActiveDocument).invoke()
                                }
                                true
                            } else false
                        }
                        .background(Theme[colors][background]),
                ) {
                    BackHandler(enabled = !showSettings) {
                        persistence.requestExit(onExit)
                    }
                    UnsavedChangesDialog()
                    HomeScreen(modifier = Modifier.fillMaxSize())
                    AnimatedVisibility(
                        visible = showSettings,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        SettingsScreen(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Theme[colors][background]),
                        )
                    }
                }
            }
        }
    }
}
