package com.klyx.terminal.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

expect class NativeTypeface

@Composable
expect fun rememberNativeTypeface(
    fontFamily: FontFamily,
    fontWeight: FontWeight = FontWeight.Normal,
    fontStyle: FontStyle = FontStyle.Normal
): State<NativeTypeface>
