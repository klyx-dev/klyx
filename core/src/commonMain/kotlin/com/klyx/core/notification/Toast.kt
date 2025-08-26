package com.klyx.core.notification

import androidx.compose.ui.graphics.vector.ImageVector

data class Toast(
    val message: String,
    val icon: ImageVector? = null,
    val durationMillis: Long = LENGTH_SHORT,
    val onDismiss: () -> Unit = {}
) {
    companion object {
        const val LENGTH_SHORT = 3000L
        const val LENGTH_LONG = 5000L
    }
}
