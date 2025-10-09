package com.klyx.editor.compose.scroll

import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFold

internal actual fun platformDefaultFlingBehavior(): ScrollableDefaultFlingBehavior =
    DefaultFlingBehavior(splineBasedDecay(UnityDensity))

@Composable
internal actual fun rememberPlatformDefaultFlingBehavior(): FlingBehavior {
    val flingSpec = rememberSplineBasedDecay<Float>()
    return remember(flingSpec) { DefaultFlingBehavior(flingSpec) }
}

internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig = AndroidScrollConfig

internal object AndroidScrollConfig : ScrollConfig {
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset =
        event.changes.fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta } * -64.dp.toPx()
}
