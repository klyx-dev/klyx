package com.klyx.api.ui.theme

import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color as AndroidColor

val LocalIsDarkMode = staticCompositionLocalOf { false }

val primaryLight = Color(0xFF415F91)
val onPrimaryLight = Color(0xFFFFFFFF)
val primaryContainerLight = Color(0xFFD6E3FF)
val onPrimaryContainerLight = Color(0xFF284777)
val secondaryLight = Color(0xFF565F71)
val onSecondaryLight = Color(0xFFFFFFFF)
val secondaryContainerLight = Color(0xFFDAE2F9)
val onSecondaryContainerLight = Color(0xFF3E4759)
val tertiaryLight = Color(0xFF705575)
val onTertiaryLight = Color(0xFFFFFFFF)
val tertiaryContainerLight = Color(0xFFFAD8FD)
val onTertiaryContainerLight = Color(0xFF573E5C)
val errorLight = Color(0xFFBA1A1A)
val onErrorLight = Color(0xFFFFFFFF)
val errorContainerLight = Color(0xFFFFDAD6)
val onErrorContainerLight = Color(0xFF93000A)
val backgroundLight = Color(0xFFF9F9FF)
val onBackgroundLight = Color(0xFF191C20)
val surfaceLight = Color(0xFFF9F9FF)
val onSurfaceLight = Color(0xFF191C20)
val surfaceVariantLight = Color(0xFFE0E2EC)
val onSurfaceVariantLight = Color(0xFF44474E)
val outlineLight = Color(0xFF74777F)
val outlineVariantLight = Color(0xFFC4C6D0)
val scrimLight = Color(0xFF000000)
val inverseSurfaceLight = Color(0xFF2E3036)
val inverseOnSurfaceLight = Color(0xFFF0F0F7)
val inversePrimaryLight = Color(0xFFAAC7FF)
val surfaceDimLight = Color(0xFFD9D9E0)
val surfaceBrightLight = Color(0xFFF9F9FF)
val surfaceContainerLowestLight = Color(0xFFFFFFFF)
val surfaceContainerLowLight = Color(0xFFF3F3FA)
val surfaceContainerLight = Color(0xFFEDEDF4)
val surfaceContainerHighLight = Color(0xFFE7E8EE)
val surfaceContainerHighestLight = Color(0xFFE2E2E9)

val primaryDark = Color(0xFFAAC7FF)
val onPrimaryDark = Color(0xFF0A305F)
val primaryContainerDark = Color(0xFF284777)
val onPrimaryContainerDark = Color(0xFFD6E3FF)
val secondaryDark = Color(0xFFBEC6DC)
val onSecondaryDark = Color(0xFF283141)
val secondaryContainerDark = Color(0xFF3E4759)
val onSecondaryContainerDark = Color(0xFFDAE2F9)
val tertiaryDark = Color(0xFFDDBCE0)
val onTertiaryDark = Color(0xFF3F2844)
val tertiaryContainerDark = Color(0xFF573E5C)
val onTertiaryContainerDark = Color(0xFFFAD8FD)
val errorDark = Color(0xFFFFB4AB)
val onErrorDark = Color(0xFF690005)
val errorContainerDark = Color(0xFF93000A)
val onErrorContainerDark = Color(0xFFFFDAD6)
val backgroundDark = Color(0xFF111318)
val onBackgroundDark = Color(0xFFE2E2E9)
val surfaceDark = Color(0xFF111318)
val onSurfaceDark = Color(0xFFE2E2E9)
val surfaceVariantDark = Color(0xFF44474E)
val onSurfaceVariantDark = Color(0xFFC4C6D0)
val outlineDark = Color(0xFF8E9099)
val outlineVariantDark = Color(0xFF44474E)
val scrimDark = Color(0xFF000000)
val inverseSurfaceDark = Color(0xFFE2E2E9)
val inverseOnSurfaceDark = Color(0xFF2E3036)
val inversePrimaryDark = Color(0xFF415F91)
val surfaceDimDark = Color(0xFF111318)
val surfaceBrightDark = Color(0xFF37393E)
val surfaceContainerLowestDark = Color(0xFF0C0E13)
val surfaceContainerLowDark = Color(0xFF191C20)
val surfaceContainerDark = Color(0xFF1D2024)
val surfaceContainerHighDark = Color(0xFF282A2F)
val surfaceContainerHighestDark = Color(0xFF33353A)

@Suppress("NOTHING_TO_INLINE")
inline fun Color.takeIf(predicate: Boolean) = if (predicate) this else Color.Unspecified

@Suppress("NOTHING_TO_INLINE")
inline fun Color.takeUnless(predicate: Boolean) = takeIf(!predicate)

fun Color.blend(
    color: Color,
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0.2f
): Color = Color(this.toArgb().blend(color.toArgb(), fraction))

@Composable
fun Color.inverse(
    fraction: (Boolean) -> Float = { 0.5f },
    darkMode: Boolean = LocalIsDarkMode.current,
): Color = if (darkMode) {
    blend(Color.White, fraction(true))
} else {
    blend(Color.Black, fraction(false))
}

fun Color.inverseByLuma(
    fraction: (Boolean) -> Float = { 0.5f },
): Color = if (luminance() < 0.3f) {
    blend(Color.White, fraction(true))
} else {
    blend(Color.Black, fraction(false))
}

@Composable
fun Color.inverse(
    fraction: (Boolean) -> Float = { 0.5f },
    color: (Boolean) -> Color,
    darkMode: Boolean = LocalIsDarkMode.current,
): Color = if (darkMode) blend(color(true), fraction(true)) else blend(color(true), fraction(false))

@ColorInt
fun @receiver:ColorInt Int.blend(
    @ColorInt other: Int,
    @FloatRange(from = 0.0, to = 1.0) fraction: Float = 0.2f
): Int {
    val inverseFraction = 1f - fraction
    val a = AndroidColor.alpha(this) * inverseFraction + AndroidColor.alpha(other) * fraction
    val r = AndroidColor.red(this) * inverseFraction + AndroidColor.red(other) * fraction
    val g = AndroidColor.green(this) * inverseFraction + AndroidColor.green(other) * fraction
    val b = AndroidColor.blue(this) * inverseFraction + AndroidColor.blue(other) * fraction
    return AndroidColor.argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
}

@Composable
@ReadOnlyComposable
fun Color.harmonizeWithPrimary(
    @FloatRange(
        from = 0.0,
        to = 1.0
    ) fraction: Float = 0.2f
): Color = blend(MaterialTheme.colorScheme.primary, fraction)

fun @receiver:ColorInt Int.toColor() = Color(this)

inline val Green: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFFBADB94).harmonizeWithPrimary(0.2f)

inline val Red: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFFE06565).harmonizeWithPrimary(0.2f)

inline val Blue: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFF0088CC).harmonizeWithPrimary(0.2f)

inline val Black: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFF142329).harmonizeWithPrimary(0.2f)

inline val StrongBlack: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFF141414).harmonizeWithPrimary(0.07f)

inline val White: Color
    @Composable
    @ReadOnlyComposable
    get() = Color(0xFFFFFFFF).harmonizeWithPrimary(0.07f)
