package com.klyx.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.klyx.api.ui.ToolbarIcon
import com.klyx.api.ui.theme.GoogleSansRounded

@Composable
fun ExpressiveMenuItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    ExpressiveMenuItem(
        text = text,
        icon = ToolbarIcon(icon),
        onClick = onClick,
        isDestructive = isDestructive
    )
}

@Composable
fun ExpressiveMenuItem(
    text: String,
    icon: Painter,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    ExpressiveMenuItem(
        text = text,
        icon = ToolbarIcon(icon),
        onClick = onClick,
        isDestructive = isDestructive
    )
}

@Composable
fun ExpressiveMenuItem(
    text: String,
    icon: ToolbarIcon?,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    val contentColor =
        if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        contentColor = contentColor,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (icon) {
                is ToolbarIcon.ImageVector -> {
                    Icon(
                        imageVector = icon.imageVector,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                is ToolbarIcon.Painter -> {
                    Icon(
                        painter = icon.painter,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                is ToolbarIcon.File -> {
                    AsyncImage(
                        model = icon.file,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }

                else -> {}
            }

            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
