package com.klyx.editor.compose.scroll

import androidx.compose.animation.core.generateDecayAnimationSpec
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
    CupertinoFlingBehavior(CupertinoScrollDecayAnimationSpec().generateDecayAnimationSpec())

@Composable
internal actual fun rememberPlatformDefaultFlingBehavior(): FlingBehavior =
// Unlike other platforms, we don't need to remember it based on density,
    // because it's density independent
    remember {
        platformDefaultFlingBehavior()
    }

internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig = UiKitScrollConfig

internal object UiKitScrollConfig : ScrollConfig {
    /*
     * There are no scroll events produced on iOS,
     * so in reality this function should not be ever called.
     * The implementation is copied from androidMain just for testing purposes.
     */
    override fun Density.calculateMouseWheelScroll(event: PointerEvent, bounds: IntSize): Offset =
        event.changes.fastFold(Offset.Zero) { acc, c -> acc + c.scrollDelta } * -64.dp.toPx()
}
