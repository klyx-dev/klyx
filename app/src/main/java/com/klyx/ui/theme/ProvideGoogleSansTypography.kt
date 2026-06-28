package com.klyx.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.klyx.api.ui.theme.GoogleSansTypography

@Composable
fun ProvideGoogleSansTypography(content: @Composable () -> Unit) {
    MaterialTheme(
        typography = GoogleSansTypography,
        content = content
    )
}
