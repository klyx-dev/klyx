package com.klyx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ProvideGoogleSansTypography(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = GoogleSansTypography,
        content = content
    )
}
