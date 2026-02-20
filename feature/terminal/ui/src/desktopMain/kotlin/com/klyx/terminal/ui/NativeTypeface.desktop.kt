package com.klyx.terminal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.skia.Typeface

actual typealias NativeTypeface = Typeface

@Composable
actual fun rememberNativeTypeface(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontStyle: FontStyle
): State<NativeTypeface> {
    val resolver = LocalFontFamilyResolver.current
    return remember(fontFamily, fontWeight, fontStyle) {
        @Suppress("UNCHECKED_CAST")
        resolver.resolve(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle
        ) as State<Typeface>
    }
}
