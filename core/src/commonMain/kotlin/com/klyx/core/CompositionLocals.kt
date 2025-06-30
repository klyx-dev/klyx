package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.file.KWatchEvent
import com.klyx.core.file.KxFile
import com.klyx.core.file.asWatchChannel
import com.klyx.core.file.isTextEqualTo
import com.klyx.core.notification.LocalNotificationManager
import com.klyx.core.settings.AppSettings
import com.klyx.core.settings.SettingsManager
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

    CompositionLocalProvider(
        LocalNotifier provides koinInject(),
        LocalNotificationManager provides koinInject(),
        LocalAppSettings provides settings,
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
