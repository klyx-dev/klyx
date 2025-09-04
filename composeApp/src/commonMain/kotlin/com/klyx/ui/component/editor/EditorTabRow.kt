package com.klyx.ui.component.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.klyx.tab.Tab

@Composable
fun EditorTabRow(
    tabs: List<Tab>,
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onClose: (Int) -> Unit = {},
    isDirty: (Int) -> Boolean = { false },
) {
    val dirty by rememberUpdatedState(isDirty)

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp))
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(4.dp)
        ) {
            itemsIndexed(tabs) { index, tab ->
                EditorTab(
                    tab = tab,
                    isSelected = index == selectedTab,
                    onClick = { onTabSelected(index) },
                    onClose = { onClose(index) },
                    isDirty = dirty(index),
                    modifier = Modifier.animateItem()
                )
            }
        }
    }
}

@Composable
fun EditorTab(
    tab: Tab,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDirty: Boolean = false,
    onClick: () -> Unit = {},
    onClose: () -> Unit = {},
) {
    Box(
        modifier = modifier.border(1.dp, MaterialTheme.colorScheme.onSurface, MaterialTheme.shapes.small),
    ) {
        val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(backgroundColor)
                .clickable { onClick() }
                .padding(horizontal = 13.dp, vertical = 7.dp)
        ) {
            AnimatedVisibility(isDirty) {
                Row {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            Text(tab.name)
            Spacer(modifier = Modifier.width(4.dp))

            AnimatedVisibility(isSelected) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onClose)
                )
            }
        }
    }
}
