package com.klyx.ui.theme

import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.api.ui.theme.GoogleSansTypography
import com.klyx.api.ui.theme.Typography
import com.klyx.api.ui.theme.backgroundDark
import com.klyx.api.ui.theme.backgroundLight
import com.klyx.api.ui.theme.errorContainerDark
import com.klyx.api.ui.theme.errorContainerLight
import com.klyx.api.ui.theme.errorDark
import com.klyx.api.ui.theme.errorLight
import com.klyx.api.ui.theme.inverseOnSurfaceDark
import com.klyx.api.ui.theme.inverseOnSurfaceLight
import com.klyx.api.ui.theme.inversePrimaryDark
import com.klyx.api.ui.theme.inversePrimaryLight
import com.klyx.api.ui.theme.inverseSurfaceDark
import com.klyx.api.ui.theme.inverseSurfaceLight
import com.klyx.api.ui.theme.onBackgroundDark
import com.klyx.api.ui.theme.onBackgroundLight
import com.klyx.api.ui.theme.onErrorContainerDark
import com.klyx.api.ui.theme.onErrorContainerLight
import com.klyx.api.ui.theme.onErrorDark
import com.klyx.api.ui.theme.onErrorLight
import com.klyx.api.ui.theme.onPrimaryContainerDark
import com.klyx.api.ui.theme.onPrimaryContainerLight
import com.klyx.api.ui.theme.onPrimaryDark
import com.klyx.api.ui.theme.onPrimaryLight
import com.klyx.api.ui.theme.onSecondaryContainerDark
import com.klyx.api.ui.theme.onSecondaryContainerLight
import com.klyx.api.ui.theme.onSecondaryDark
import com.klyx.api.ui.theme.onSecondaryLight
import com.klyx.api.ui.theme.onSurfaceDark
import com.klyx.api.ui.theme.onSurfaceLight
import com.klyx.api.ui.theme.onSurfaceVariantDark
import com.klyx.api.ui.theme.onSurfaceVariantLight
import com.klyx.api.ui.theme.onTertiaryContainerDark
import com.klyx.api.ui.theme.onTertiaryContainerLight
import com.klyx.api.ui.theme.onTertiaryDark
import com.klyx.api.ui.theme.onTertiaryLight
import com.klyx.api.ui.theme.outlineDark
import com.klyx.api.ui.theme.outlineLight
import com.klyx.api.ui.theme.outlineVariantDark
import com.klyx.api.ui.theme.outlineVariantLight
import com.klyx.api.ui.theme.primaryContainerDark
import com.klyx.api.ui.theme.primaryContainerLight
import com.klyx.api.ui.theme.primaryDark
import com.klyx.api.ui.theme.primaryLight
import com.klyx.api.ui.theme.scrimDark
import com.klyx.api.ui.theme.scrimLight
import com.klyx.api.ui.theme.secondaryContainerDark
import com.klyx.api.ui.theme.secondaryContainerLight
import com.klyx.api.ui.theme.secondaryDark
import com.klyx.api.ui.theme.secondaryLight
import com.klyx.api.ui.theme.surfaceBrightDark
import com.klyx.api.ui.theme.surfaceBrightLight
import com.klyx.api.ui.theme.surfaceContainerDark
import com.klyx.api.ui.theme.surfaceContainerHighDark
import com.klyx.api.ui.theme.surfaceContainerHighLight
import com.klyx.api.ui.theme.surfaceContainerHighestDark
import com.klyx.api.ui.theme.surfaceContainerHighestLight
import com.klyx.api.ui.theme.surfaceContainerLight
import com.klyx.api.ui.theme.surfaceContainerLowDark
import com.klyx.api.ui.theme.surfaceContainerLowLight
import com.klyx.api.ui.theme.surfaceContainerLowestDark
import com.klyx.api.ui.theme.surfaceContainerLowestLight
import com.klyx.api.ui.theme.surfaceDark
import com.klyx.api.ui.theme.surfaceDimDark
import com.klyx.api.ui.theme.surfaceDimLight
import com.klyx.api.ui.theme.surfaceLight
import com.klyx.api.ui.theme.surfaceVariantDark
import com.klyx.api.ui.theme.surfaceVariantLight
import com.klyx.api.ui.theme.tertiaryContainerDark
import com.klyx.api.ui.theme.tertiaryContainerLight
import com.klyx.api.ui.theme.tertiaryDark
import com.klyx.api.ui.theme.tertiaryLight
import com.klyx.ui.animation.LocalReduceMotion
import com.klyx.ui.animation.orSnap

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

private fun ColorScheme.applyAmoled(): ColorScheme {
    return this.copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF121212)
    )
}

@Composable
fun KlyxTheme(
    darkTheme: Boolean = LocalIsDarkMode.current,
    amoled: Boolean = false,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit
) {

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkScheme
        else -> lightScheme
    }

    val reduceMotion = LocalReduceMotion.current
    val finalColorScheme = if (darkTheme && amoled) colorScheme.applyAmoled() else colorScheme

    // Google Sans Rounded has no Devanagari glyphs; render those locales in Laila.
    val typography = if (isDevanagariLocale()) {
        GoogleSansTypography.withLailaFont()
    } else {
        Typography
    }

    MaterialExpressiveTheme(
        colorScheme = finalColorScheme,
        typography = typography,
        motionScheme = reducedMotionScheme(reduceMotion),
        content = content
    )
}

private fun reducedMotionScheme(reduceMotion: Boolean) = object : MotionScheme {

    val expressive = MotionScheme.expressive()

    override fun <T> defaultSpatialSpec(): FiniteAnimationSpec<T> {
        return expressive.defaultSpatialSpec<T>().orSnap(reduceMotion)
    }

    override fun <T> fastSpatialSpec(): FiniteAnimationSpec<T> {
        return expressive.fastSpatialSpec<T>().orSnap(reduceMotion)
    }

    override fun <T> slowSpatialSpec(): FiniteAnimationSpec<T> {
        return expressive.slowSpatialSpec<T>().orSnap(reduceMotion)
    }

    override fun <T> defaultEffectsSpec(): FiniteAnimationSpec<T> {
        return expressive.defaultEffectsSpec<T>().orSnap(reduceMotion)
    }

    override fun <T> fastEffectsSpec(): FiniteAnimationSpec<T> {
        return expressive.fastEffectsSpec<T>().orSnap(reduceMotion)
    }

    override fun <T> slowEffectsSpec(): FiniteAnimationSpec<T> {
        return expressive.slowEffectsSpec<T>().orSnap(reduceMotion)
    }
}

@Composable
fun KlyxThemeSurface(content: @Composable BoxScope.() -> Unit) {
    KlyxTheme(amoled = LocalAppSettings.current.appearance.amoledDarkMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            content = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    content = content
                )
            }
        )
    }
}
