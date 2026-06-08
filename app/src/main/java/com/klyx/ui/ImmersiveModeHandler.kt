package com.klyx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
import com.klyx.util.findActivity
import com.klyx.util.isGestureNavigation

val LocalImmersiveMode = compositionLocalOf { false }

@Composable
fun ImmersiveModeHandler(
    isImmersiveModeEnabled: Boolean,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        LaunchedEffect(isImmersiveModeEnabled) {
            val window = context.findActivity()?.window ?: return@LaunchedEffect
            val windowInsetsController = WindowCompat.getInsetsController(window, view)

            if (isImmersiveModeEnabled) {
                windowInsetsController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

                if (context.isGestureNavigation()) {
                    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
                } else {
                    windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
                }
            } else {
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    CompositionLocalProvider(
        value = LocalImmersiveMode provides isImmersiveModeEnabled,
        content = content
    )
}
