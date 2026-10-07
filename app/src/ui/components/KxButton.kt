package com.klyx.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.composeunstyled.ProvideContentColor
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.theme.Theme
import com.klyx.ui.accent
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.disabledSurface
import com.klyx.ui.disabledText
import com.klyx.ui.label
import com.klyx.ui.onAccent
import com.klyx.ui.typography

enum class KxButtonVariant {
    Filled,
    Outlined,
    Ghost,
}

@Composable
fun KxButtonSurface(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    pressed: Boolean = false,
    variant: KxButtonVariant = KxButtonVariant.Filled,
    backgroundColor: Color? = null,
    borderColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    content: @Composable () -> Unit,
) {
    val shape = RectangleShape

    val background: Color = when {
        !enabled && variant == KxButtonVariant.Ghost -> Color.Transparent
        !enabled -> Theme[colors][disabledSurface]
        variant == KxButtonVariant.Filled -> Theme[colors][accent]
        backgroundColor != null -> backgroundColor
        else -> Color.Transparent
    }

    val effectiveBorderColor: Color = borderColor ?: Theme[colors][border]

    val border: BorderStroke? = when {
        !enabled && variant != KxButtonVariant.Ghost -> BorderStroke(1.dp, Theme[colors][border])
        variant == KxButtonVariant.Outlined -> BorderStroke(1.dp, effectiveBorderColor)
        else -> null
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(background, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .defaultMinSize(minHeight = 36.dp)
            .alpha(if (pressed && enabled) 0.7f else 1f)
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun KxButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: KxButtonVariant = KxButtonVariant.Filled,
    bracketed: Boolean = false,
    borderColor: Color? = null,
    backgroundColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
    contentAlignment: Alignment = Alignment.Center,
    testTag: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    contentDescription: String? = null,
    content: @Composable () -> Unit,
) {
    val pressed by interactionSource.collectIsPressedAsState()

    val bracketColor: Color = when {
        !enabled -> Theme[colors][disabledText]
        variant == KxButtonVariant.Filled -> Theme[colors][onAccent]
        borderColor != null -> borderColor
        else -> Theme[colors][accent]
    }

    val semanticsModifier = Modifier.semantics {
        if (testTag != null) {
            this.testTag = testTag
        }
        if (contentDescription != null) {
            this.contentDescription = contentDescription
        }
    }

    val shape = RectangleShape

    val background: Color = when {
        !enabled && variant == KxButtonVariant.Ghost -> Color.Transparent
        !enabled -> Theme[colors][disabledSurface]
        variant == KxButtonVariant.Filled -> Theme[colors][accent]
        backgroundColor != null -> backgroundColor
        else -> Color.Transparent
    }

    val effectiveBorderColor: Color = borderColor ?: Theme[colors][border]

    val border: BorderStroke? = when {
        !enabled && variant != KxButtonVariant.Ghost -> BorderStroke(1.dp, Theme[colors][border])
        variant == KxButtonVariant.Outlined -> BorderStroke(1.dp, effectiveBorderColor)
        else -> null
    }

    val alpha by animateFloatAsState(if (pressed && enabled) 0.7f else 1f)

    UnstyledButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = contentPadding,
        indication = indication,
        interactionSource = interactionSource,
        contentAlignment = contentAlignment,
        modifier = modifier
            .then(semanticsModifier)
            .clip(shape)
            .background(background, shape)
            .then(if (border != null) Modifier.border(border, shape) else Modifier)
            .alpha(alpha)
    ) {
        if (bracketed) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("[", style = Theme[typography][label], color = bracketColor)
                ProvideContentColor(bracketColor) {
                    content()
                }
                Text("]", style = Theme[typography][label], color = bracketColor)
            }
        } else {
            ProvideContentColor(bracketColor) {
                content()
            }
        }
    }
}

val KxButtonDefaultMinHeight = 36.dp
