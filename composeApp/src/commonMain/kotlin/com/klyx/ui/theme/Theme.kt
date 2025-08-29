package com.klyx.ui.theme

import androidx.compose.runtime.Composable

@Composable
expect fun KlyxTheme(
    themeName: String? = null,
    content: @Composable () -> Unit
)
