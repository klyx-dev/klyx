package com.klyx.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import com.klyx.LocalWindowSizeClass

@Composable
fun AdaptiveLayout(
    expandedLayout: (@Composable () -> Unit)? = null,
    mediumLayout: (@Composable () -> Unit)? = null,
    compactLayout: @Composable () -> Unit
) {
    val windowSizeClass = LocalWindowSizeClass.current

    AnimatedContent(targetState = windowSizeClass) { targetSizeClass ->
        when {
            targetSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
                expandedLayout?.invoke() ?: mediumLayout?.invoke() ?: compactLayout()
            }

            targetSizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
                mediumLayout?.invoke() ?: compactLayout()
            }

            else -> compactLayout()
        }
    }
}
