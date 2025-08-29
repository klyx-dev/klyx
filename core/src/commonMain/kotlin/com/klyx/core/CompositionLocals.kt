package com.klyx.core

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.file.KxFile
import com.klyx.core.file.watchAndReload
import com.klyx.core.notification.LocalNotificationManager
import com.klyx.core.settings.AppSettings
import com.klyx.core.settings.SettingsManager
import com.klyx.core.theme.Appearance
import com.klyx.core.theme.LocalContrast
import com.klyx.core.theme.LocalIsDarkMode
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import org.koin.compose.koinInject

fun noLocalProvidedFor(name: String?): Nothing {
    error("CompositionLocal: $name not present")
}

inline fun <reified T : Any> noLocalProvidedFor(): Nothing = noLocalProvidedFor(T::class.simpleName)

@Composable
expect fun PlatformLocalProvider(content: @Composable () -> Unit)

@Composable
fun SharedLocalProvider(content: @Composable () -> Unit) {
    val settings by SettingsManager.settings
    val settingsFile = remember { KxFile(Environment.SettingsFilePath) }
    val scope = rememberCoroutineScope()

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    DisposableEffect(Unit) {
        SettingsManager.load()
        val dispatcher = newSingleThreadContext("Settings")

        with(scope) {
            settingsFile.watchAndReload(dispatcher) { SettingsManager.load() }
        }

        onDispose { dispatcher.close() }
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()

    val darkMode by remember {
        derivedStateOf {
            when (settings.appearance) {
                Appearance.Dark -> true
                Appearance.Light -> false
                Appearance.System -> isSystemInDarkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalNotifier provides koinInject(),
        LocalNotificationManager provides koinInject(),
        LocalAppSettings provides settings,
        LocalIsDarkMode provides darkMode,
        LocalContrast provides settings.contrast
    ) {
        PlatformLocalProvider(content)
    }
}

val LocalAppSettings = compositionLocalOf<AppSettings> {
    noLocalProvidedFor<AppSettings>()
}

val LocalBuildVariant = staticCompositionLocalOf<BuildVariant> {
    noLocalProvidedFor<BuildVariant>()
}
