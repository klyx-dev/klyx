package com.klyx.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun BackButton(onClick: () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current

    IconButton(
        onClick = {
            onClick()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
        }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Back",
        )
    }
}
