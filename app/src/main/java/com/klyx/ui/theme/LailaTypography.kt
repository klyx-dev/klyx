package com.klyx.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.klyx.R
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.ui.theme.GoogleSansRounded

/**
 * https://fonts.google.com/specimen/Laila
 */
val LailaFontFamily = FontFamily(
    Font(R.font.laila_light, FontWeight.Light),
    Font(R.font.laila_regular, FontWeight.Normal),
    Font(R.font.laila_medium, FontWeight.Medium),
    Font(R.font.laila_semibold, FontWeight.SemiBold),
    Font(R.font.laila_bold, FontWeight.Bold)
)

/** Languages written in the Devanagari script. */
val DevanagariLanguages = setOf("hi", "mr", "ne", "mai", "kok", "sa", "doi")

/** Whether the effective app language (in-app override, else system locale) uses the Devanagari script. */
@Composable
fun isDevanagariLocale(): Boolean {
    val languageTag = LocalAppSettings.current.appearance.language.languageTag
        ?: Locale.current.toLanguageTag()
    return languageTag.substringBefore('-') in DevanagariLanguages
}

/**
 * Font family for text styles declared outside MaterialTheme.typography.
 * Returns Laila for Devanagari locales (Google Sans Rounded has no Devanagari
 * glyphs), Google Sans Rounded otherwise.
 */
@Composable
fun uiFontFamily(): FontFamily = if (isDevanagariLocale()) LailaFontFamily else GoogleSansRounded

/** Returns a copy of this [Typography] with every style set to [LailaFontFamily]. */
fun Typography.withLailaFont(): Typography = copy(
    displayLarge = displayLarge.copy(fontFamily = LailaFontFamily),
    displayMedium = displayMedium.copy(fontFamily = LailaFontFamily),
    displaySmall = displaySmall.copy(fontFamily = LailaFontFamily),
    headlineLarge = headlineLarge.copy(fontFamily = LailaFontFamily),
    headlineMedium = headlineMedium.copy(fontFamily = LailaFontFamily),
    headlineSmall = headlineSmall.copy(fontFamily = LailaFontFamily),
    titleLarge = titleLarge.copy(fontFamily = LailaFontFamily),
    titleMedium = titleMedium.copy(fontFamily = LailaFontFamily),
    titleSmall = titleSmall.copy(fontFamily = LailaFontFamily),
    bodyLarge = bodyLarge.copy(fontFamily = LailaFontFamily),
    bodyMedium = bodyMedium.copy(fontFamily = LailaFontFamily),
    bodySmall = bodySmall.copy(fontFamily = LailaFontFamily),
    labelLarge = labelLarge.copy(fontFamily = LailaFontFamily),
    labelMedium = labelMedium.copy(fontFamily = LailaFontFamily),
    labelSmall = labelSmall.copy(fontFamily = LailaFontFamily)
)
