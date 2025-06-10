package com.klyx.core

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.resolveAsTypeface

@Composable
fun rememberTypefaceFromFontFamily(fontFamily: FontFamily, resolver: FontFamily.Resolver = LocalFontFamilyResolver.current): State<Typeface> {
    return remember(resolver, fontFamily) { resolver.resolveAsTypeface(fontFamily) }
}

fun createFontRequestQuery(
    name: String,
    weight: FontWeight = FontWeight.W400,
    style: FontStyle = FontStyle.Normal,
    bestEffort: Boolean = false
) = "name=$name&weight=${weight.weight}&italic=${style.toQueryParam()}&besteffort=${bestEffortQueryParam(bestEffort)}"

private fun FontStyle.toQueryParam(): Int = if (this == FontStyle.Italic) 1 else 0
private fun bestEffortQueryParam(bestEffort: Boolean) = if (bestEffort) "true" else "false"
