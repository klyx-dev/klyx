package com.klyx.core.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import kotlinx.serialization.Serializable

val LocalIsDarkMode = compositionLocalOf { true }
val LocalContrast = compositionLocalOf { Contrast.Normal }

fun Theme?.orDefault() = this ?: OceanBreeze.asTheme()

data class Theme(
    val name: String,
    val lightScheme: ColorScheme,
    val lightSchemeMediumContrast: ColorScheme,
    val lightSchemeHighContrast: ColorScheme,
    val darkScheme: ColorScheme,
    val darkSchemeMediumContrast: ColorScheme,
    val darkSchemeHighContrast: ColorScheme
)

fun ThemeJson.asTheme(): Theme {
    val lightScheme = parseColorScheme("light")
    val lightSchemeMediumContrast = parseColorScheme("light-medium-contrast")
    val lightSchemeHighContrast = parseColorScheme("light-high-contrast")
    val darkScheme = parseColorScheme("dark", true)
    val darkSchemeMediumContrast = parseColorScheme("dark-medium-contrast", true)
    val darkSchemeHighContrast = parseColorScheme("dark-high-contrast", true)

    return Theme(
        name = name,
        lightScheme = lightScheme,
        lightSchemeMediumContrast = lightSchemeMediumContrast,
        lightSchemeHighContrast = lightSchemeHighContrast,
        darkScheme = darkScheme,
        darkSchemeMediumContrast = darkSchemeMediumContrast,
        darkSchemeHighContrast = darkSchemeHighContrast
    )
}

@Serializable
data class ThemeJson(
    val name: String,
    val schemes: Map<String, Map<String, String>>
)

private fun ThemeJson.parseColorScheme(
    schemeKey: String,
    isDarkScheme: Boolean = false
): ColorScheme {
    val colorsMap = schemes[schemeKey] ?: error("Scheme '$schemeKey' not found")
    val colors = colorsMap.mapValues { (_, value) -> value.toColor() }

    return if (isDarkScheme) {
        darkColorScheme(
            primary = colors["primary"]!!,
            onPrimary = colors["onPrimary"]!!,
            primaryContainer = colors["primaryContainer"]!!,
            onPrimaryContainer = colors["onPrimaryContainer"]!!,
            secondary = colors["secondary"]!!,
            onSecondary = colors["onSecondary"]!!,
            secondaryContainer = colors["secondaryContainer"]!!,
            onSecondaryContainer = colors["onSecondaryContainer"]!!,
            tertiary = colors["tertiary"]!!,
            onTertiary = colors["onTertiary"]!!,
            tertiaryContainer = colors["tertiaryContainer"]!!,
            onTertiaryContainer = colors["onTertiaryContainer"]!!,
            error = colors["error"]!!,
            onError = colors["onError"]!!,
            errorContainer = colors["errorContainer"]!!,
            onErrorContainer = colors["onErrorContainer"]!!,
            background = colors["background"]!!,
            onBackground = colors["onBackground"]!!,
            surface = colors["surface"]!!,
            onSurface = colors["onSurface"]!!,
            surfaceVariant = colors["surfaceVariant"]!!,
            onSurfaceVariant = colors["onSurfaceVariant"]!!,
            outline = colors["outline"]!!,
            outlineVariant = colors["outlineVariant"]!!,
            scrim = colors["scrim"]!!,
            inverseSurface = colors["inverseSurface"]!!,
            inverseOnSurface = colors["inverseOnSurface"]!!,
            inversePrimary = colors["inversePrimary"]!!,
            surfaceTint = colors["surfaceTint"] ?: colors["primary"]!!,
            surfaceDim = colors["surfaceDim"]!!,
            surfaceBright = colors["surfaceBright"]!!,
            surfaceContainerLowest = colors["surfaceContainerLowest"]!!,
            surfaceContainerLow = colors["surfaceContainerLow"]!!,
            surfaceContainer = colors["surfaceContainer"]!!,
            surfaceContainerHigh = colors["surfaceContainerHigh"]!!,
            surfaceContainerHighest = colors["surfaceContainerHighest"]!!,
        )
    } else {
        lightColorScheme(
            primary = colors["primary"]!!,
            onPrimary = colors["onPrimary"]!!,
            primaryContainer = colors["primaryContainer"]!!,
            onPrimaryContainer = colors["onPrimaryContainer"]!!,
            secondary = colors["secondary"]!!,
            onSecondary = colors["onSecondary"]!!,
            secondaryContainer = colors["secondaryContainer"]!!,
            onSecondaryContainer = colors["onSecondaryContainer"]!!,
            tertiary = colors["tertiary"]!!,
            onTertiary = colors["onTertiary"]!!,
            tertiaryContainer = colors["tertiaryContainer"]!!,
            onTertiaryContainer = colors["onTertiaryContainer"]!!,
            error = colors["error"]!!,
            onError = colors["onError"]!!,
            errorContainer = colors["errorContainer"]!!,
            onErrorContainer = colors["onErrorContainer"]!!,
            background = colors["background"]!!,
            onBackground = colors["onBackground"]!!,
            surface = colors["surface"]!!,
            onSurface = colors["onSurface"]!!,
            surfaceVariant = colors["surfaceVariant"]!!,
            onSurfaceVariant = colors["onSurfaceVariant"]!!,
            outline = colors["outline"]!!,
            outlineVariant = colors["outlineVariant"]!!,
            scrim = colors["scrim"]!!,
            inverseSurface = colors["inverseSurface"]!!,
            inverseOnSurface = colors["inverseOnSurface"]!!,
            inversePrimary = colors["inversePrimary"]!!,
            surfaceTint = colors["surfaceTint"] ?: colors["primary"]!!,
            surfaceDim = colors["surfaceDim"]!!,
            surfaceBright = colors["surfaceBright"]!!,
            surfaceContainerLowest = colors["surfaceContainerLowest"]!!,
            surfaceContainerLow = colors["surfaceContainerLow"]!!,
            surfaceContainer = colors["surfaceContainer"]!!,
            surfaceContainerHigh = colors["surfaceContainerHigh"]!!,
            surfaceContainerHighest = colors["surfaceContainerHighest"]!!,
        )
    }
}
