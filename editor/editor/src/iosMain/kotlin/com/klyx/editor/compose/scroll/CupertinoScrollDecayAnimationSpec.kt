package com.klyx.editor.compose.scroll

import androidx.compose.animation.core.FloatDecayAnimationSpec
import platform.UIKit.UIScrollViewDecelerationRateNormal
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * A class that represents the animation specification for a scroll decay animation
 * using iOS-style decay behavior.
 *
 * @property decelerationRate The rate at which the velocity decelerates over time.
 * Default value is equal to one used by default UIScrollView behavior.
 */
internal class CupertinoScrollDecayAnimationSpec(
    private val decelerationRate: Float = UIScrollViewDecelerationRateNormal.toFloat()
) : FloatDecayAnimationSpec {

    private val coefficient: Float = 1000f * ln(decelerationRate)

    override val absVelocityThreshold: Float = 0.5f // Half pixel

    override fun getTargetValue(initialValue: Float, initialVelocity: Float): Float =
        initialValue - initialVelocity / coefficient

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeSeconds = convertNanosToSeconds(playTimeNanos).toFloat()
        val initialVelocityOverTimeIntegral =
            (decelerationRate.pow(1000f * playTimeSeconds) - 1f) / coefficient * initialVelocity
        return initialValue + initialVelocityOverTimeIntegral
    }

    override fun getDurationNanos(initialValue: Float, initialVelocity: Float): Long {
        val absVelocity = abs(initialVelocity)

        if (absVelocity < absVelocityThreshold) {
            return 0
        }

        val seconds = ln(-coefficient * absVelocityThreshold / absVelocity) / coefficient

        return convertSecondsToNanos(seconds)
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeSeconds = convertNanosToSeconds(playTimeNanos).toFloat()

        return initialVelocity * decelerationRate.pow(1000f * playTimeSeconds)
    }
}

internal const val SecondsToNanos: Long = 1_000_000_000L

internal fun convertSecondsToNanos(seconds: Float): Long =
    (seconds.toDouble() * SecondsToNanos).roundToLong()

internal fun convertNanosToSeconds(nanos: Long): Double =
    nanos.toDouble() / SecondsToNanos
