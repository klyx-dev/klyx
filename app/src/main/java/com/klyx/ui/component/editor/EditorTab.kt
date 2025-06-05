package com.klyx.ui.component.editor

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun EditorTabBar(
    tabs: List<String>,
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onClose: (Int) -> Unit = {},
    isDirty: (Int) -> Boolean = { false },
) {
    LazyRow(modifier = modifier.zIndex(5f)
        .fillMaxWidth()) {
        itemsIndexed(tabs) { index, tab ->
            EditorTab(
                text = tab,
                isSelected = index == selectedTab,
                isDirty = isDirty(index),
                onClick = { onTabSelected(index) },
                onClose = { onClose(index) }
            )
        }
    }
}

@Composable
fun EditorTab(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDirty: Boolean = false,
    onClick: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    val surface4 = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
    val onSurface4 = contentColorFor(surface4)
    val surface1 = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
    val onSurface1 = contentColorFor(surface1)

    val backgroundColor by animateColorAsState(
        if (isSelected) surface4 else surface1,
        label = "TabBackgroundColor"
    )

    val textColor = if (isSelected) onSurface4 else onSurface1

    Row(
        modifier = modifier
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDirty) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(Color.Red, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }

        Text(
            text = text,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodySmall
            //modifier = Modifier.weight(1f, fill = false)
        )

        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close tab",
            tint = textColor,
            modifier = Modifier
                .size(18.dp)
                .clickable(onClick = onClose)
                .padding(start = 8.dp)
        )
    }
}

