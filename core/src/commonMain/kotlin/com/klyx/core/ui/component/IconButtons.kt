@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.klyx.core.ui.component

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.klyx.icons.ArrowBack
import com.klyx.icons.Icons

@Composable
fun BackButton(onClick: () -> Unit) {
    val hapticFeedback = LocalHapticFeedback.current

    IconButton(
        onClick = {
            onClick()
            hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick)
        },
        shapes = IconButtonDefaults.shapes()
    ) {
        Icon(
            imageVector = Icons.ArrowBack,
            contentDescription = "Back",
        )
    }
}
