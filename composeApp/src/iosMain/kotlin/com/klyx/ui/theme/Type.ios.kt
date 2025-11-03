package com.klyx.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import com.klyx.core.file.KxFile

actual val bodyFontFamily: FontFamily
    get() = FontFamily.Default

actual val displayFontFamily: FontFamily
    get() = FontFamily.Default

@Composable
actual fun rememberFontFamily(name: String): FontFamily {
    return FontFamily.Default
}

actual fun KxFile.resolveFontFamily(): FontFamily = FontFamily.Default
