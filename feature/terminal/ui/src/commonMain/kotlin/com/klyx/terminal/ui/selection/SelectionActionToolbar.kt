package com.klyx.terminal.ui.selection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

@Composable
fun SelectionActionToolbar(
    visible: Boolean,
    anchorX: Float,
    anchorY: Float,
    canPaste: Boolean,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = contentColorFor(backgroundColor),
    toolbarHeightPx: Float = 128f,   // pixels above anchorY to float the bar
) {
    if (!visible) return

    val offsetY = (anchorY - toolbarHeightPx).roundToInt().coerceAtLeast(0)

    Popup(
        offset = IntOffset(anchorX.roundToInt(), offsetY),
        properties = PopupProperties(focusable = false),
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + scaleIn(initialScale = 0.85f),
            exit = fadeOut() + scaleOut(targetScale = 0.85f),
        ) {
            ToolbarRow(
                modifier = modifier,
                backgroundColor = backgroundColor,
                contentColor = contentColor,
                canPaste = canPaste,
                onCopy = onCopy,
                onPaste = onPaste,
                onMore = onMore,
            )
        }
    }
}

@Composable
private fun ToolbarRow(
    canPaste: Boolean,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onMore: () -> Unit,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(6.dp)

    Row(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape)
            .clip(shape)
            .background(backgroundColor)
            .height(40.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarButton(label = "Copy", color = contentColor, onClick = onCopy)

        ToolbarDivider(color = contentColor)

        ToolbarButton(
            label = "Paste",
            color = if (canPaste) contentColor else contentColor.copy(alpha = 0.38f),
            enabled = canPaste,
            onClick = onPaste,
        )

        ToolbarDivider(color = contentColor)

        ToolbarButton(label = "More...", color = contentColor, onClick = onMore)
    }
}

@Composable
private fun ToolbarButton(
    label: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Text(
        text = label,
        color = color,
        fontSize = 14.sp,
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun ToolbarDivider(color: Color) {
    Spacer(modifier = Modifier.width(1.dp))
    VerticalDivider(
        modifier = Modifier.height(24.dp),
        thickness = 1.dp,
        color = color.copy(alpha = 0.25f),
    )
    Spacer(modifier = Modifier.width(1.dp))
}
