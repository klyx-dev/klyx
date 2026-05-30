package com.klyx.presentation.components.slider

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.util.packFloats
import androidx.compose.ui.util.unpackFloat1
import androidx.compose.ui.util.unpackFloat2

/**
 * Immutable float range for [CustomRangeSlider]
 *
 * Used in [CustomRangeSlider] to determine the active track range for the component.
 * The range is as follows: SliderRange.start..SliderRange.endInclusive.
 */
@Immutable
@JvmInline
internal value class CustomSliderRange(
    val packedValue: Long
) {
    /**
     * start of the [CustomSliderRange]
     */
    @Stable
    val start: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) {
                "SliderRange is unspecified"
            }
            return unpackFloat1(packedValue)
        }

    /**
     * End (inclusive) of the [CustomSliderRange]
     */
    @Stable
    val endInclusive: Float
        get() {
            // Explicitly compare against packed values to avoid auto-boxing of Size.Unspecified
            check(this.packedValue != Unspecified.packedValue) {
                "SliderRange is unspecified"
            }
            return unpackFloat2(packedValue)
        }

    companion object {
        /**
         * Represents an unspecified [CustomSliderRange] value, usually a replacement for `null`
         * when a primitive value is desired.
         */
        @Stable
        val Unspecified = CustomSliderRange(Float.NaN, Float.NaN)
    }

    /**
     * String representation of the [CustomSliderRange]
     */
    override fun toString() = if (isSpecified) {
        "$start..$endInclusive"
    } else {
        "FloatRange.Unspecified"
    }
}

/**
 * Creates a [CustomSliderRange] from a given start and endInclusive float.
 * It requires endInclusive to be >= start.
 *
 * @param start float that indicates the start of the range
 * @param endInclusive float that indicates the end of the range
 */
@Stable
internal fun CustomSliderRange(
    start: Float,
    endInclusive: Float
): CustomSliderRange {
    val isUnspecified = start.isNaN() && endInclusive.isNaN()
    require(isUnspecified || start <= endInclusive) {
        "start($start) must be <= endInclusive($endInclusive)"
    }
    return CustomSliderRange(packFloats(start, endInclusive))
}

/**
 * Creates a [CustomSliderRange] from a given [ClosedFloatingPointRange].
 * It requires range.endInclusive >= range.start.
 *
 * @param range the ClosedFloatingPointRange<Float> for the range.
 */
@Stable
internal fun CustomSliderRange(range: ClosedFloatingPointRange<Float>): CustomSliderRange {
    val start = range.start
    val endInclusive = range.endInclusive
    val isUnspecified = start.isNaN() && endInclusive.isNaN()
    require(isUnspecified || start <= endInclusive) {
        "ClosedFloatingPointRange<Float>.start($start) must be <= " +
                "ClosedFloatingPoint.endInclusive($endInclusive)"
    }
    return CustomSliderRange(packFloats(start, endInclusive))
}

/**
 * Check for if a given [CustomSliderRange] is not [CustomSliderRange.Unspecified].
 */
@Stable
internal val CustomSliderRange.isSpecified: Boolean
    get() =
        packedValue != CustomSliderRange.Unspecified.packedValue