package com.klyx.core

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.core.file.toKxFile
import com.klyx.core.file.watchAndReload
import com.klyx.core.io.Paths
import com.klyx.core.io.settingsFile
import com.klyx.core.notification.LocalNotificationManager
import com.klyx.core.settings.LocalAppSettings
import com.klyx.core.settings.SettingsManager
import com.klyx.core.settings.paletteStyles
import com.klyx.core.theme.Appearance
import com.klyx.core.theme.DEFAULT_SEED_COLOR
import com.klyx.core.theme.FixedColorRoles
import com.klyx.core.theme.LocalContrast
import com.klyx.core.theme.LocalIsDarkMode
import com.kyant.monet.LocalTonalPalettes
import com.kyant.monet.PaletteStyle
import com.kyant.monet.TonalPalettes.Companion.toTonalPalettes
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
    val settings by SettingsManager.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    @OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    DisposableEffect(Unit) {
        val dispatcher = newSingleThreadContext("Settings")

        scope.launch { SettingsManager.load() }
            .invokeOnCompletion {
                scope.launch(Dispatchers.Default) {
                    val settingsFile = Paths.settingsFile.toKxFile()
                    settingsFile.watchAndReload(dispatcher) { SettingsManager.load() }
                }
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

    val tonalPalettes = if (settings.dynamicColor) {
        dynamicDarkColorScheme().toTonalPalettes()
    } else {
        Color(settings.seedColor).toTonalPalettes(
            paletteStyles.getOrElse(settings.paletteStyleIndex) { PaletteStyle.TonalSpot }
        )
    }

    val density = LocalDensity.current.density

    CompositionLocalProvider(
        LocalDensity provides Density(density, settings.fontScale),
        LocalNotifier provides koinInject(),
        LocalNotificationManager provides koinInject(),
        LocalAppSettings provides settings,
        LocalIsDarkMode provides darkMode,
        LocalContrast provides settings.contrast,
        LocalSeedColor provides settings.seedColor,
        LocalPaletteStyleIndex provides settings.paletteStyleIndex,
        LocalTonalPalettes provides tonalPalettes
    ) {
        PlatformLocalProvider(content)
    }
}

@Composable
expect fun dynamicDarkColorScheme(): ColorScheme

@Composable
expect fun dynamicLightColorScheme(): ColorScheme

val LocalBuildVariant = staticCompositionLocalOf<BuildVariant> {
    noLocalProvidedFor<BuildVariant>()
}

val LocalFixedColorRoles = staticCompositionLocalOf {
    FixedColorRoles.fromColorSchemes(
        lightColors = lightColorScheme(),
        darkColors = darkColorScheme(),
    )
}

val LocalSeedColor = compositionLocalOf { DEFAULT_SEED_COLOR }
val LocalPaletteStyleIndex = compositionLocalOf { 0 }
