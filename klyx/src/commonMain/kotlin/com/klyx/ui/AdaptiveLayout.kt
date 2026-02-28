package com.klyx.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.Composable
import androidx.window.core.layout.WindowSizeClass
import com.klyx.LocalWindowSizeClass

@Composable
fun AdaptiveLayout(
    mediumLayout: (@Composable () -> Unit)? = null,
    expandedLayout: (@Composable () -> Unit)? = null,
    largeLayout: (@Composable () -> Unit)? = null,
    extraLargeLayout: (@Composable () -> Unit)? = null,
    compactLayout: @Composable () -> Unit,
) {
    val windowSizeClass = LocalWindowSizeClass.current

    AnimatedContent(targetState = windowSizeClass) { targetClass ->
        when {
            targetClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND) -> {
                extraLargeLayout?.invoke() ?: largeLayout?.invoke() ?: expandedLayout?.invoke()
                ?: mediumLayout?.invoke() ?: compactLayout()
            }

            targetClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> {
                largeLayout?.invoke() ?: expandedLayout?.invoke() ?: mediumLayout?.invoke() ?: compactLayout()
            }

            targetClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> {
                expandedLayout?.invoke() ?: mediumLayout?.invoke() ?: compactLayout()
            }

            targetClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> {
                mediumLayout?.invoke() ?: compactLayout()
            }

            else -> compactLayout()
        }
    }
}
