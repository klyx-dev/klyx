package com.klyx.editor.compose.scroll

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.DecayAnimationSpec
import androidx.compose.animation.core.animateDecay
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.ui.MotionDurationScale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlin.math.abs

internal class CupertinoFlingBehavior(
    private val flingDecay: DecayAnimationSpec<Float>,
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale,

    /*
     * Post-drag inertia with velocity below [velocityThreshold] value will be consumed entirely
     * and not trigger any fling at all, value is approx and reverse-engineered from iOS 16 UIScrollView
     * blackbox
     */
    private val velocityThreshold: Float = 500f
) : ScrollableDefaultFlingBehavior {
    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        if (abs(initialVelocity) < velocityThreshold) {
            return 0f
        }

        // come up with the better threshold, but we need it since spline curve gives us NaNs
        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f
                AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity,
                ).animateDecay(flingDecay) {
                    val delta = value - lastValue
                    val consumed = try {
                        scrollBy(delta)
                    } catch (_: CancellationException) {
                        0.0f
                    }
                    lastValue = value
                    velocityLeft = this.velocity
                    // avoid rounding errors and stop if anything is unconsumed
                    if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                }
                velocityLeft
            } else {
                initialVelocity
            }
        }
    }
}
