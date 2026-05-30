package com.klyx.ui.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp

val LocalScreenSize = compositionLocalOf<ScreenSize> { error("ScreenSize not present") }

@ConsistentCopyVisibility
data class ScreenSize internal constructor(
    val width: Dp,
    val height: Dp,
    val widthPx: Int,
    val heightPx: Int
)

@Composable
fun rememberScreenSize(): ScreenSize {
    val windowInfo = LocalWindowInfo.current

    return remember(windowInfo) {
        derivedStateOf {
            windowInfo.run {
                ScreenSize(
                    width = containerDpSize.width,
                    height = containerDpSize.height,
                    widthPx = containerSize.width,
                    heightPx = containerSize.height
                )
            }
        }
    }.value
}
