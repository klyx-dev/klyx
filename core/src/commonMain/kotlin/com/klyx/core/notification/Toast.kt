package com.klyx.core.notification

import androidx.compose.ui.graphics.vector.ImageVector

data class Toast(
    val message: String,
    val icon: ImageVector? = null,
    val durationMillis: Long = 3000L,
    val onDismiss: () -> Unit = {}
)
