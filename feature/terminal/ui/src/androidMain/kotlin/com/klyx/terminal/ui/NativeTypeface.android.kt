package com.klyx.terminal.ui

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface

actual typealias NativeTypeface = Typeface

@Composable
actual fun rememberNativeTypeface(
    fontFamily: FontFamily,
    fontWeight: FontWeight,
    fontStyle: FontStyle
): State<NativeTypeface> {
    val resolver = LocalFontFamilyResolver.current
    return remember(fontFamily, fontWeight, fontStyle) {
        resolver.resolveAsTypeface(
            fontFamily = fontFamily,
            fontWeight = fontWeight,
            fontStyle = fontStyle
        )
    }
}
