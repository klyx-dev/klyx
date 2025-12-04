@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.klyx.ui.component.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.klyx.core.ui.component.DropdownMenuDivider
import com.klyx.tab.Tab
import com.klyx.tab.TabMenuAction
import com.klyx.tab.TabMenuState

@Composable
fun EditorTabRow(
    tabs: List<Tab>,
    modifier: Modifier = Modifier,
    selectedTab: Int = 0,
    onTabSelected: (index: Int) -> Unit = {},
    onTabMenuAction: (TabMenuAction) -> Unit = {},
    tabMenuState: TabMenuState = TabMenuState(),
    isDirty: (index: Int) -> Boolean = { false },
) {
    val dirty by rememberUpdatedState(isDirty)

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        itemsIndexed(tabs) { index, tab ->
            EditorTab(
                tab = tab,
                index = index,
                tabMenuState = tabMenuState,
                isSelected = index == selectedTab,
                onClick = { onTabSelected(index) },
                onClose = { onTabMenuAction(TabMenuAction.Close(index)) },
                isDirty = dirty(index),
                onCloseOthers = { onTabMenuAction(TabMenuAction.CloseOthers(index)) },
                onCloseLeft = { onTabMenuAction(TabMenuAction.CloseLeft(index)) },
                onCloseRight = { onTabMenuAction(TabMenuAction.CloseRight(index)) },
                onCloseAll = { onTabMenuAction(TabMenuAction.CloseAll(index)) },
                onCopyPath = { onTabMenuAction(TabMenuAction.CopyPath(index)) },
                onCopyRelativePath = { onTabMenuAction(TabMenuAction.CopyRelativePath(index)) },
                //modifier = Modifier.animateItem()
            )
        }
    }
}

@Composable
private fun EditorTab(
    tab: Tab,
    index: Int,
    tabMenuState: TabMenuState,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    isDirty: Boolean = false,
    onClick: () -> Unit = {},
    onClose: () -> Unit = {},
    onCloseOthers: () -> Unit = {},
    onCloseLeft: () -> Unit = {},
    onCloseRight: () -> Unit = {},
    onCloseAll: () -> Unit = {},
    onCopyPath: () -> Unit = {},
    onCopyRelativePath: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .border(
                width = Dp.Hairline,
                color = MaterialTheme.colorScheme.onSurface,
                shape = MaterialTheme.shapes.small
            ),
    ) {
        val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Unspecified
        var showDropdown by remember { mutableStateOf(false) }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(backgroundColor)
                .combinedClickable(
                    onClick = { onClick(); if (isSelected) showDropdown = true },
                    onLongClick = { showDropdown = true }
                )
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

            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
            )
        }

        EditorTabMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            isCloseLeftEnabled = tabMenuState.enabled(TabMenuAction.CloseLeft(index)),
            isCloseRightEnabled = tabMenuState.enabled(TabMenuAction.CloseRight(index)),
            isCloseOthersEnabled = tabMenuState.enabled(TabMenuAction.CloseOthers(index)),
            isCopyPathVisible = tabMenuState.visible(TabMenuAction.CopyPath(index)),
            isCopyRelativePathVisible = tabMenuState.visible(TabMenuAction.CopyRelativePath(index)),
            onClose = onClose,
            onCloseOthers = onCloseOthers,
            onCloseLeft = onCloseLeft,
            onCloseRight = onCloseRight,
            onCloseAll = onCloseAll,
            onCopyPath = onCopyPath,
            onCopyRelativePath = onCopyRelativePath
        )
    }
}

@Composable
private fun EditorTabMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    isCopyPathVisible: Boolean = true,
    isCopyRelativePathVisible: Boolean = true,
    isCloseLeftEnabled: Boolean = false,
    isCloseRightEnabled: Boolean = false,
    isCloseOthersEnabled: Boolean = false,
    onClose: () -> Unit = {},
    onCloseOthers: () -> Unit = {},
    onCloseLeft: () -> Unit = {},
    onCloseRight: () -> Unit = {},
    onCloseAll: () -> Unit = {},
    onCopyPath: () -> Unit = {},
    onCopyRelativePath: () -> Unit = {},
) {
    DropdownMenu(
        expanded = expanded,
        shape = MaterialTheme.shapes.small,
        //border = BorderStroke(Dp.Hairline, MaterialTheme.colorScheme.outline),
        modifier = Modifier.heightIn(max = 250.dp),
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = { Text("Close") },
            onClick = {
                onClose()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            enabled = isCloseOthersEnabled,
            text = { Text("Close Others") },
            onClick = {
                onCloseOthers()
                onDismissRequest()
            }
        )

        DropdownMenuDivider()

        DropdownMenuItem(
            enabled = isCloseLeftEnabled,
            text = { Text("Close Left") },
            onClick = {
                onCloseLeft()
                onDismissRequest()
            }
        )

        DropdownMenuItem(
            enabled = isCloseRightEnabled,
            text = { Text("Close Right") },
            onClick = {
                onCloseRight()
                onDismissRequest()
            }
        )

        DropdownMenuDivider()

        DropdownMenuItem(
            text = { Text("Close All") },
            onClick = {
                onCloseAll()
                onDismissRequest()
            }
        )

        if (isCopyPathVisible || isCopyRelativePathVisible) {
            DropdownMenuDivider()
        }

        if (isCopyPathVisible) {
            DropdownMenuItem(
                text = { Text("Copy Path") },
                onClick = {
                    onCopyPath()
                    onDismissRequest()
                }
            )
        }

        if (isCopyRelativePathVisible) {
            DropdownMenuItem(
                text = { Text("Copy Relative Path") },
                onClick = {
                    onCopyRelativePath()
                    onDismissRequest()
                }
            )
        }
    }
}
