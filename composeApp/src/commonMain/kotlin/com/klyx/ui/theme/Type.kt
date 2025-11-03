@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.klyx.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextDirection
import com.klyx.core.file.KxFile
import com.klyx.res.IBMPlexSans_Bold
import com.klyx.res.IBMPlexSans_BoldItalic
import com.klyx.res.IBMPlexSans_ExtraLight
import com.klyx.res.IBMPlexSans_ExtraLightItalic
import com.klyx.res.IBMPlexSans_Italic
import com.klyx.res.IBMPlexSans_Light
import com.klyx.res.IBMPlexSans_LightItalic
import com.klyx.res.IBMPlexSans_Medium
import com.klyx.res.IBMPlexSans_MediumItalic
import com.klyx.res.IBMPlexSans_Regular
import com.klyx.res.IBMPlexSans_SemiBold
import com.klyx.res.IBMPlexSans_SemiBoldItalic
import com.klyx.res.IBMPlexSans_Text
import com.klyx.res.IBMPlexSans_TextItalic
import com.klyx.res.IBMPlexSans_Thin
import com.klyx.res.IBMPlexSans_ThinItalic
import com.klyx.res.JetBrainsMono_Bold
import com.klyx.res.JetBrainsMono_Bold_Italic
import com.klyx.res.JetBrainsMono_ExtraBold
import com.klyx.res.JetBrainsMono_ExtraBold_Italic
import com.klyx.res.JetBrainsMono_Italic
import com.klyx.res.JetBrainsMono_Medium
import com.klyx.res.JetBrainsMono_Medium_Italic
import com.klyx.res.JetBrainsMono_Regular
import com.klyx.res.Lilex_Bold
import com.klyx.res.Lilex_BoldItalic
import com.klyx.res.Lilex_ExtraLight
import com.klyx.res.Lilex_ExtraLightItalic
import com.klyx.res.Lilex_Italic
import com.klyx.res.Lilex_Light
import com.klyx.res.Lilex_LightItalic
import com.klyx.res.Lilex_Medium
import com.klyx.res.Lilex_MediumItalic
import com.klyx.res.Lilex_Regular
import com.klyx.res.Lilex_SemiBold
import com.klyx.res.Lilex_SemiBoldItalic
import com.klyx.res.Lilex_Thin
import com.klyx.res.Lilex_ThinItalic
import com.klyx.res.Res
import org.jetbrains.compose.resources.Font

expect val bodyFontFamily: FontFamily
expect val displayFontFamily: FontFamily

@Composable
expect fun rememberFontFamily(name: String): FontFamily

expect fun KxFile.resolveFontFamily(): FontFamily

inline val KlyxSans
    @Composable
    get() = IBMPlexSansFontFamily

inline val KlyxMono
    @Composable
    get() = LilexFontFamily

val Typography
    @Composable
    get() = IBMPlexSansTypography()

val LilexFontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.Lilex_Thin, weight = FontWeight.Thin),
        Font(Res.font.Lilex_ThinItalic, weight = FontWeight.Thin, style = FontStyle.Italic),
        Font(Res.font.Lilex_ExtraLight, weight = FontWeight.ExtraLight),
        Font(Res.font.Lilex_ExtraLightItalic, weight = FontWeight.ExtraLight, style = FontStyle.Italic),
        Font(Res.font.Lilex_Light, weight = FontWeight.Light),
        Font(Res.font.Lilex_LightItalic, weight = FontWeight.Light, style = FontStyle.Italic),
        Font(Res.font.Lilex_Italic, style = FontStyle.Italic),
        Font(Res.font.Lilex_Regular, weight = FontWeight.Normal),
        Font(Res.font.Lilex_Medium, weight = FontWeight.Medium),
        Font(Res.font.Lilex_MediumItalic, weight = FontWeight.Medium, style = FontStyle.Italic),
        Font(Res.font.Lilex_SemiBold, weight = FontWeight.SemiBold),
        Font(Res.font.Lilex_SemiBoldItalic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
        Font(Res.font.Lilex_Bold, weight = FontWeight.Bold),
        Font(Res.font.Lilex_BoldItalic, weight = FontWeight.Bold, style = FontStyle.Italic),
    )

val IBMPlexSansFontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.IBMPlexSans_Thin, weight = FontWeight.Thin),
        Font(Res.font.IBMPlexSans_ThinItalic, weight = FontWeight.Thin, style = FontStyle.Italic),
        Font(Res.font.IBMPlexSans_ExtraLight, weight = FontWeight.ExtraLight),
        Font(Res.font.IBMPlexSans_ExtraLightItalic, weight = FontWeight.ExtraLight, style = FontStyle.Italic),
        Font(Res.font.IBMPlexSans_Light, weight = FontWeight.Light),
        Font(Res.font.IBMPlexSans_LightItalic, weight = FontWeight.Light, style = FontStyle.Italic),
        Font(Res.font.IBMPlexSans_Text, weight = FontWeight.Normal),
        Font(Res.font.IBMPlexSans_TextItalic, weight = FontWeight.Normal, style = FontStyle.Italic),
        Font(Res.font.IBMPlexSans_Italic, style = FontStyle.Italic),
        Font(Res.font.IBMPlexSans_Regular, weight = FontWeight.Normal),
        Font(Res.font.IBMPlexSans_Medium, weight = FontWeight.Medium),
        Font(Res.font.IBMPlexSans_MediumItalic, weight = FontWeight.Medium, style = FontStyle.Italic),
        Font(Res.font.IBMPlexSans_SemiBold, weight = FontWeight.SemiBold),
        Font(Res.font.IBMPlexSans_SemiBoldItalic, weight = FontWeight.SemiBold, style = FontStyle.Italic),
        Font(Res.font.IBMPlexSans_Bold, weight = FontWeight.Bold),
        Font(Res.font.IBMPlexSans_BoldItalic, weight = FontWeight.Bold, style = FontStyle.Italic),
    )

val JetBrainsMonoFontFamily
    @Composable
    get() = FontFamily(
        Font(Res.font.JetBrainsMono_Italic, style = FontStyle.Italic),
        Font(Res.font.JetBrainsMono_Regular, weight = FontWeight.Normal),
        Font(Res.font.JetBrainsMono_Medium, weight = FontWeight.Medium),
        Font(Res.font.JetBrainsMono_Medium_Italic, weight = FontWeight.Medium, style = FontStyle.Italic),
        Font(Res.font.JetBrainsMono_ExtraBold, weight = FontWeight.ExtraBold),
        Font(Res.font.JetBrainsMono_ExtraBold_Italic, weight = FontWeight.ExtraBold, style = FontStyle.Italic),
        Font(Res.font.JetBrainsMono_Bold, weight = FontWeight.Bold),
        Font(Res.font.JetBrainsMono_Bold_Italic, weight = FontWeight.Bold, style = FontStyle.Italic),
    )

@Suppress("ComposableNaming")
@Composable
internal fun IBMPlexSansTypography() = Typography().copy(IBMPlexSansFontFamily)

@Suppress("ComposableNaming")
@Composable
internal fun LilexTypography() = Typography().copy(LilexFontFamily)

fun Typography.copy(fontFamily: FontFamily) = copy(
    displayLarge = displayLarge.custom(fontFamily),
    displayMedium = displayMedium.custom(fontFamily),
    displaySmall = displaySmall.custom(fontFamily),
    headlineLarge = headlineLarge.custom(fontFamily),
    headlineMedium = headlineMedium.custom(fontFamily),
    headlineSmall = headlineSmall.custom(fontFamily),
    titleLarge = titleLarge.custom(fontFamily),
    titleMedium = titleMedium.custom(fontFamily),
    titleSmall = titleSmall.custom(fontFamily),
    bodyLarge = bodyLarge.custom(fontFamily),
    bodyMedium = bodyMedium.custom(fontFamily),
    bodySmall = bodySmall.custom(fontFamily),
    labelLarge = labelLarge.custom(fontFamily),
    labelMedium = labelMedium.custom(fontFamily),
    labelSmall = labelSmall.custom(fontFamily),
    displayLargeEmphasized = displayLargeEmphasized.custom(fontFamily),
    displayMediumEmphasized = displayMediumEmphasized.custom(fontFamily),
    displaySmallEmphasized = displaySmallEmphasized.custom(fontFamily),
    headlineLargeEmphasized = headlineLargeEmphasized.custom(fontFamily),
    headlineMediumEmphasized = headlineMediumEmphasized.custom(fontFamily),
    headlineSmallEmphasized = headlineSmallEmphasized.custom(fontFamily),
    titleLargeEmphasized = titleLargeEmphasized.custom(fontFamily),
    titleMediumEmphasized = titleMediumEmphasized.custom(fontFamily),
    titleSmallEmphasized = titleSmallEmphasized.custom(fontFamily),
    bodyLargeEmphasized = bodyLargeEmphasized.custom(fontFamily),
    bodyMediumEmphasized = bodyMediumEmphasized.custom(fontFamily),
    bodySmallEmphasized = bodySmallEmphasized.custom(fontFamily),
    labelLargeEmphasized = labelLargeEmphasized.custom(fontFamily),
    labelMediumEmphasized = labelMediumEmphasized.custom(fontFamily),
    labelSmallEmphasized = labelSmallEmphasized.custom(fontFamily),
)

private fun TextStyle.custom(
    family: FontFamily,
    weight: FontWeight? = this.fontWeight
): TextStyle = this.copy(
    fontFamily = family,
    fontWeight = weight,
    lineBreak = LineBreak.Paragraph,
    textDirection = TextDirection.Content
)
