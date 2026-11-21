package com.klyx.ui.theme

import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composeunstyled.theme.ColorScheme
import com.composeunstyled.theme.Theme
import com.composeunstyled.theme.ThemeProperty
import com.composeunstyled.theme.ThemeToken
import com.composeunstyled.theme.buildThemeV2
import klyx.resources.Res
import klyx.resources.fira_code_regular
import klyx.resources.geist_mono_regular
import klyx.resources.hack_regular
import klyx.resources.jetbrains_mono_regular
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

val ColorScheme.Companion.Mocha: ColorScheme get() = ColorScheme("mocha")

val LocalColorScheme = compositionLocalOf { ColorScheme.Mocha }

val colors = ThemeProperty<Color>("colors")

val background = ThemeToken<Color>("background")
val surface = ThemeToken<Color>("surface")
val surfaceVariant = ThemeToken<Color>("surface_variant")
val border = ThemeToken<Color>("border")
val textPrimary = ThemeToken<Color>("text_primary")
val textSecondary = ThemeToken<Color>("text_secondary")
val textMuted = ThemeToken<Color>("text_muted")
val accent = ThemeToken<Color>("accent")
val onAccent = ThemeToken<Color>("on_accent")
val accentSecondary = ThemeToken<Color>("accent_secondary")
val error = ThemeToken<Color>("error")
val success = ThemeToken<Color>("success")
val warning = ThemeToken<Color>("warning")
val disabledSurface = ThemeToken<Color>("disabled_surface")
val disabledText = ThemeToken<Color>("disabled_text")


val typography = ThemeProperty<TextStyle>("typography")

val headline = ThemeToken<TextStyle>("headline")
val title = ThemeToken<TextStyle>("title")
val body = ThemeToken<TextStyle>("body")
val bodySmall = ThemeToken<TextStyle>("body_small")
val label = ThemeToken<TextStyle>("label")
val labelSmall = ThemeToken<TextStyle>("label_small")

object KxTypography {
    @get:Composable
    val headline: TextStyle
        get() = Theme[typography][com.klyx.ui.theme.headline]

    @get:Composable
    val title: TextStyle
        get() = Theme[typography][com.klyx.ui.theme.title]

    @get:Composable
    val body: TextStyle
        get() = Theme[typography][com.klyx.ui.theme.body]

    @get:Composable
    val bodySmall: TextStyle
        get() = Theme[typography][com.klyx.ui.theme.bodySmall]

    @get:Composable
    val label: TextStyle
        get() = Theme[typography][com.klyx.ui.theme.label]

    @get:Composable
    val labelSmall: TextStyle
        get() = Theme[typography][com.klyx.ui.theme.labelSmall]
}

val KlyxTheme = buildThemeV2 {
    name = "KlyxTheme"
    colorSchemeTransitionSpec = tween(200)
    defaultIndication = ripple(color = Color.Black)

    val colors = LocalKxColors.current
    properties[com.klyx.ui.theme.colors] = mapOf(
        background to colors.background,
        surface to colors.surface,
        surfaceVariant to colors.surfaceVariant,
        border to colors.border,
        textPrimary to colors.textPrimary,
        textSecondary to colors.textSecondary,
        textMuted to colors.textMuted,
        accent to colors.accent,
        accentSecondary to colors.accentSecondary,
        error to colors.error,
        success to colors.success,
        warning to colors.warning,
        onAccent to colors.onAccent,
        disabledSurface to colors.disabledSurface,
        disabledText to colors.disabledText,
    )

    defaultContentColor = colors.textPrimary

    val mono = currentFontFamily(LocalKxFont.current)
    properties[typography] = mapOf(
        headline to TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ),
        title to TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        body to TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        bodySmall to TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        ),
        label to TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp,
        ),
        labelSmall to TextStyle(
            fontFamily = mono,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        ),
    )

    defaultTextStyle = properties[typography][body]
}

val LocalKxFont = staticCompositionLocalOf { FontOption.Default }

val LocalCornerRadius = compositionLocalOf { 0.dp }

@Composable
fun kxShape(radius: Dp = LocalCornerRadius.current): Shape =
    if (radius <= 0.dp) RectangleShape else RoundedCornerShape(radius)

@Composable
fun kxSmallShape(radius: Dp = (LocalCornerRadius.current / 2).coerceAtLeast(0.dp)): Shape =
    if (radius <= 0.dp) RectangleShape else RoundedCornerShape(radius)

val LocalCornerShape: Shape
    @Composable
    get() = kxShape()

@Composable
fun currentFontFamily(option: FontOption = LocalKxFont.current): FontFamily = FontFamily(
    Font(option.resource, FontWeight.Normal),
    Font(option.resource, FontWeight.Medium),
    Font(option.resource, FontWeight.SemiBold),
)

enum class FontOption(
    val id: String,
    val label: String,
    val resource: FontResource
) {
    JetBrainsMono("jetbrains_mono", "JetBrains Mono", Res.font.jetbrains_mono_regular),
    FiraCode("fira_code", "Fira Code", Res.font.fira_code_regular),
    Hack("hack", "Hack", Res.font.hack_regular),
    GeistMono("geist_mono", "Geist Mono", Res.font.geist_mono_regular);

    companion object {
        val Default = JetBrainsMono

        fun fromId(id: String?): FontOption {
            if (id.isNullOrBlank()) return Default
            return entries.firstOrNull { it.id == id } ?: Default
        }
    }
}
