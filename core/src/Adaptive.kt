package com.klyx.core

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.window.core.layout.WindowSizeClass

/** Window shape of the host, for layouts that adapt to size class. Must be provided at the root. */
val LocalWindowAdaptiveInfo = compositionLocalOf<WindowAdaptiveInfo> {
    error("No WindowAdaptiveInfo provided")
}

val LocalWindowSizeClass = compositionLocalWithComputedDefaultOf {
    LocalWindowAdaptiveInfo.currentValue.windowSizeClass
}

val LocalWindowPosture = compositionLocalWithComputedDefaultOf {
    LocalWindowAdaptiveInfo.currentValue.windowPosture
}

/** Provides [LocalWindowAdaptiveInfo] for the composition. */
@Composable
fun ProvideWindowAdaptiveInfo(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalWindowAdaptiveInfo provides currentWindowAdaptiveInfoV2(),
        content = content
    )
}

fun WindowSizeClass.isWidthAtLeastMediumBreakpoint() =
    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND)

fun WindowSizeClass.isWidthAtLeastExpandedBreakpoint() =
    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

fun WindowSizeClass.isWidthAtLeastLargeBreakpoint() =
    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND)

fun WindowSizeClass.isWidthAtLeastExtraLargeBreakpoint() =
    isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND)

fun WindowSizeClass.isHeightAtLeastMediumBreakpoint() =
    isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND)
