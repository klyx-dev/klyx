package com.klyx.core

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.file.KWatchEvent
import com.klyx.core.file.KxFile
import com.klyx.core.file.LocalFileDownloader
import com.klyx.core.file.asWatchChannel
import com.klyx.core.file.isTextEqualTo
import com.klyx.core.notification.LocalNotificationManager
import com.klyx.core.settings.AppSettings
import com.klyx.core.settings.SettingsManager
import com.klyx.core.theme.Appearance
import com.klyx.core.theme.LocalIsDarkMode
import com.klyx.core.theme.ThemeManager
import com.klyx.core.theme.isDark
import kotlinx.coroutines.channels.consumeEach
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

    LaunchedEffect(Unit) {
        SettingsManager.load()
        println(settingsFile.absolutePath)

        var oldContent = settingsFile.readText()

        settingsFile.asWatchChannel().consumeEach { event ->
            if (event.kind == KWatchEvent.Kind.Modified) {
                if (!event.file.isTextEqualTo(oldContent)) {
                    SettingsManager.load()
                    oldContent = event.file.readText()
                }
            }
        }
    }

    val isSystemInDarkTheme = isSystemInDarkTheme()

    val darkMode by remember {
        derivedStateOf {
            if (settings.dynamicColor) {
                when (settings.appearance) {
                    Appearance.Dark -> true
                    Appearance.Light -> false
                    Appearance.System -> isSystemInDarkTheme
                }
            } else {
                ThemeManager.getThemeByName(settings.theme)?.isDark() ?: isSystemInDarkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalNotifier provides koinInject(),
        LocalNotificationManager provides koinInject(),
        LocalFileDownloader provides koinInject(),
        LocalAppSettings provides settings,
        LocalIsDarkMode provides darkMode
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
