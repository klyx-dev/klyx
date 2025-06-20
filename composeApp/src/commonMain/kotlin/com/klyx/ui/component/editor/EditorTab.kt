package com.klyx.ui.component.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.klyx.tab.Tab

@Composable
fun EditorTabBar(
    tabs: List<Tab>,
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onClose: (Int) -> Unit = {},
    isDirty: (Int) -> Boolean = { false },
) {
    val dirty by rememberUpdatedState(isDirty)

    Box(
        modifier = modifier
            .zIndex(5f)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp))
            .fillMaxWidth()
    ) {
        LazyRow {
            itemsIndexed(tabs) { index, tab ->
                EditorTab(
                    tab = tab,
                    isSelected = index == selectedTab,
                    isDirty = dirty(index),
                    onClick = { onTabSelected(index) },
                    onClose = { onClose(index) }
                )
            }
        }
    }
}

@Composable
expect fun EditorTab(
    tab: Tab,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDirty: Boolean = false,
    onClick: () -> Unit = {},
    onClose: () -> Unit = {},
)

