package com.klyx.presentation.components.slider

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.ripple
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun FancySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    thumbShape: Shape = MaterialShapes.Cookie12Sided.toShape(),
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    val thumbColor by animateColorAsState(
        targetValue = contentColorFor(if (enabled) colors.activeTrackColor else colors.disabledActiveTrackColor),
        label = "thumbColor"
    )

    val thumb: @Composable (CustomSliderState) -> Unit = { sliderState ->
        val sliderFraction by remember(value, sliderState) {
            derivedStateOf {
                (value - sliderState.valueRange.start) / (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
            }
        }

        val rotation by animateFloatAsState(360f * sliderFraction)

        Spacer(
            Modifier
                .zIndex(100f)
                .rotate(rotation)
                .size(26.dp)
                .indication(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = false, radius = 24.dp)
                )
                .hoverable(interactionSource = interactionSource)
                .shadow(elevation = 1.dp, shape = thumbShape)
                .background(thumbColor, thumbShape)
        )
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        CustomSlider(
            interactionSource = interactionSource,
            thumb = thumb,
            enabled = enabled,
            modifier = modifier,//.fancyContainer(drawContainer), // Replaced .container
            colors = colors.toCustom(),
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            steps = steps,
            track = { sliderState ->
                CustomSliderDefaults.Track(
                    sliderState = sliderState,
                    colors = colors.toCustom(),
                    trackHeight = 38.dp,
                    enabled = enabled
                )
            }
        )
    }
}

@Composable
fun FancyRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    colors: SliderColors,
    startInteractionSource: MutableInteractionSource,
    endInteractionSource: MutableInteractionSource,
    thumbShape: Shape,
    modifier: Modifier = Modifier,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0
) {
    val thumbColor by animateColorAsState(
        targetValue = if (enabled) colors.thumbColor else colors.disabledThumbColor,
        label = "thumbColor"
    )

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
        CustomRangeSlider(
            startInteractionSource = startInteractionSource,
            endInteractionSource = endInteractionSource,
            enabled = enabled,
            modifier = modifier,
            colors = colors.toCustom(),
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = valueRange,
            startThumb = {
                Spacer(
                    Modifier
                        .zIndex(100f)
                        .size(26.dp)
                        .indication(
                            interactionSource = startInteractionSource,
                            indication = ripple(bounded = false, radius = 24.dp)
                        )
                        .hoverable(interactionSource = startInteractionSource)
                        .shadow(elevation = 1.dp, shape = thumbShape)
                        .background(thumbColor, thumbShape)
                )
            },
            endThumb = {
                Spacer(
                    Modifier
                        .zIndex(100f)
                        .size(26.dp)
                        .indication(
                            interactionSource = endInteractionSource,
                            indication = ripple(bounded = false, radius = 24.dp)
                        )
                        .hoverable(interactionSource = endInteractionSource)
                        .shadow(elevation = 1.dp, shape = thumbShape)
                        .background(thumbColor, thumbShape)
                )
            },
            steps = steps,
            track = { sliderState ->
                CustomSliderDefaults.Track(
                    rangeSliderState = sliderState,
                    colors = colors.toCustom(),
                    trackHeight = 38.dp,
                    enabled = enabled
                )
            }
        )
    }
}

@Stable
internal fun SliderColors.toCustom(): CustomSliderColors = CustomSliderColors(
    thumbColor = thumbColor,
    activeTrackColor = activeTrackColor,
    activeTickColor = activeTickColor,
    inactiveTrackColor = inactiveTrackColor,
    inactiveTickColor = inactiveTickColor,
    disabledThumbColor = disabledThumbColor,
    disabledActiveTrackColor = disabledActiveTrackColor,
    disabledActiveTickColor = disabledActiveTickColor,
    disabledInactiveTrackColor = disabledInactiveTrackColor,
    disabledInactiveTickColor = disabledInactiveTickColor
)
