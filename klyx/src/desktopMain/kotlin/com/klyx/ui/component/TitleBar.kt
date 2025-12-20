package com.klyx.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Close
import androidx.compose.material.icons.sharp.CropSquare
import androidx.compose.material.icons.sharp.Fullscreen
import androidx.compose.material.icons.sharp.Minimize
import androidx.compose.material.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.klyx.minimize
import com.klyx.toggleMaximizeRestore

@Composable
fun TitleBar(
    scope: FrameWindowScope,
    state: WindowState,
    onCloseRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMinimized by remember { derivedStateOf { state.placement == WindowPlacement.Floating } }
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(32.dp)
            .background(colorScheme.surfaceContainerHigh),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompositionLocalProvider(
            LocalContentColor provides colorScheme.onSurface
        ) {
            ClickableIcon(
                Icons.Sharp.Minimize,
                onClick = scope::minimize,
                modifier = Modifier.size(40.dp),
                contentDescription = "Minimize"
            )

            ClickableIcon(
                if (!isMinimized) {
                    Icons.Sharp.CropSquare
                } else {
                    Icons.Sharp.Fullscreen
                },
                onClick = scope::toggleMaximizeRestore,
                modifier = Modifier.size(40.dp),
                contentDescription = if (!isMinimized) "Restore" else "Maximize"
            )

            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()

            ClickableIcon(
                Icons.Sharp.Close,
                onClick = onCloseRequest,
                modifier = Modifier.size(40.dp),
                tint = if (isHovered) colorScheme.onErrorContainer else colorScheme.onSurface,
                backgroundColor = if (isHovered) colorScheme.errorContainer else Color.Transparent,
                interactionSource = interactionSource,
                contentDescription = "Close"
            )
        }
    }
}

@Composable
private fun ClickableIcon(
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    contentDescription: String? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    backgroundColor: Color = Color.Transparent
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = tint
        )
    }
}
