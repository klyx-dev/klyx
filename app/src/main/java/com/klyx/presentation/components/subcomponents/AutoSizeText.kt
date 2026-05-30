package com.klyx.presentation.components.subcomponents

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    minFontSize: TextUnit = 8.sp,
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    maxFontSizeLimit: TextUnit = 100.sp, // Practical upper limit for the search
    lineHeightRatio: Float = 1.2f // Factor for line height (e.g., 1.2f for 20% more space)
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var currentFontSize by remember { mutableStateOf(minFontSize) }
    var readyToDraw by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.toPx() }.toInt()
        val maxHeightPx = with(density) { maxHeight.toPx() }.toInt()

        LaunchedEffect(
            text,
            style,
            minFontSize,
            maxFontSizeLimit,
            lineHeightRatio,
            maxWidthPx,
            maxHeightPx
        ) {
            readyToDraw = false
            var bestFitFontSize = minFontSize // We start assuming the minimum.

            // Ensure the limits for the search are valid.
            var lowerBoundSp = minFontSize.value
            var upperBoundSp = maxFontSizeLimit.value.coerceAtLeast(minFontSize.value)

            // If the search range is invalid (e.g., min > max limit), we use minFontSize.
            if (lowerBoundSp > upperBoundSp + 0.01f) {
                currentFontSize = minFontSize
                readyToDraw = true
                return@LaunchedEffect
            }

            val minFontEffectiveLineHeight = minFontSize * lineHeightRatio
            val minFontEffectiveStyle = style.copy(
                fontSize = minFontSize,
                lineHeight = minFontEffectiveLineHeight
            )
            val minFontLayoutResult = textMeasurer.measure(
                text = AnnotatedString(text),
                style = minFontEffectiveStyle,
                overflow = TextOverflow.Clip, // We use Clip for precise measurement.
                softWrap = true,
                maxLines = Int.MAX_VALUE, // Allow all necessary lines.
                constraints = Constraints(
                    maxWidth = maxWidthPx.coerceAtLeast(0),
                    maxHeight = maxHeightPx.coerceAtLeast(0)
                )
            )

            if (minFontLayoutResult.hasVisualOverflow) {
                // Even with minFontSize, the text overflows. We will use minFontSize and it will be truncated.
                currentFontSize = minFontSize
                readyToDraw = true
                return@LaunchedEffect
            } else {
                // minFontSize fits, so it's our initial "best fit".
                bestFitFontSize = minFontSize
            }

            repeat(15) { // 15 iterations are usually enough for precision in sp.
                // If the difference between the limits is very small, we have converged.
                if (upperBoundSp - lowerBoundSp < 0.1f) {
                    currentFontSize = bestFitFontSize
                    readyToDraw = true
                    return@LaunchedEffect
                }

                val midSp = (lowerBoundSp + upperBoundSp) / 2f
                val candidateFontSize = midSp.sp

                // Avoid measuring sizes smaller than our known best fit, if we already passed them.
                if (candidateFontSize.value < bestFitFontSize.value && candidateFontSize.value < midSp) {
                    lowerBoundSp = midSp + 0.01f // Continue search in the upper half.
                    return@repeat
                }

                // Calculate the lineHeight dynamically based on the candidateFontSize.
                val currentEffectiveLineHeight = candidateFontSize * lineHeightRatio
                val candidateStyle = style.copy(
                    fontSize = candidateFontSize,
                    lineHeight = currentEffectiveLineHeight
                )

                val layoutResult = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = candidateStyle,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                    constraints = Constraints(
                        maxWidth = maxWidthPx.coerceAtLeast(0),
                        maxHeight = maxHeightPx.coerceAtLeast(0)
                    )
                )

                if (layoutResult.hasVisualOverflow) {
                    // The candidate size is too large (overflows in height or width).
                    upperBoundSp = midSp - 0.01f
                } else {
                    // The candidate size fits. It is our new "best fit".
                    // We will try to find an even larger one.
                    bestFitFontSize = candidateFontSize
                    lowerBoundSp = midSp + 0.01f
                }
            }

            currentFontSize = bestFitFontSize
            readyToDraw = true
        }

        // We only draw the Text once we have determined the font size.
        if (readyToDraw) {
            val finalEffectiveLineHeight = currentFontSize * lineHeightRatio
            Text(
                text = text,
                modifier = Modifier, // The Text modifier doesn't need fillMaxSize here.
                style = style.copy(
                    fontSize = currentFontSize,
                    lineHeight = finalEffectiveLineHeight
                ),
                fontFamily = fontFamily,
                fontWeight = fontWeight,
                overflow = TextOverflow.Ellipsis, // Truncate if, despite everything, it still overflows.
                softWrap = true,
                // The font size was chosen so that all lines fit in height.
                maxLines = Int.MAX_VALUE
            )
        }
    }
}
