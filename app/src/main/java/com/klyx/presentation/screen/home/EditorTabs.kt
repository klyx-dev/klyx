package com.klyx.presentation.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.app.icons.CloseFullscreen
import com.klyx.app.icons.DeleteSweep
import com.klyx.i18n.strings
import com.klyx.presentation.components.AnimatedTab
import com.klyx.ui.theme.uiFontFamily
import kotlinx.collections.immutable.ImmutableList

@Composable
fun EditorTabs(
    openTabs: ImmutableList<WorkspaceTab>,
    activeTab: WorkspaceTab?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabCloseOthers: (String) -> Unit,
    onTabCloseAll: () -> Unit
) {
    val activeTabIndex = remember(openTabs, activeTab) {
        val index = openTabs.indexOfFirst { it.id == activeTab?.id }
        if (index != -1) index else 0
    }

    PrimaryScrollableTabRow(
        selectedTabIndex = activeTabIndex,
        containerColor = Transparent,
        edgePadding = 4.dp,
        indicator = {},
        divider = {},
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        openTabs.fastForEachIndexed { index, tab ->
            val isActive = index == activeTabIndex
            var isMenuExpanded by remember { mutableStateOf(false) }

            val isModified = tab is WorkspaceTab.TextFile && tab.hasUnsavedChanges

            AnimatedTab(
                index = index,
                selectedIndex = activeTabIndex,
                onClick = {
                    if (isActive) {
                        isMenuExpanded = true
                    } else {
                        onTabClick(tab.id)
                    }
                },
            ) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(
                                start = 6.dp,
                                top = 2.dp,
                                bottom = 2.dp,
                                end = 2.dp
                            )
                            .animateContentSize()
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = uiFontFamily(),
                            fontWeight = if (index == activeTabIndex) FontWeight.SemiBold else FontWeight.Medium
                        )

                        AnimatedVisibility(
                            visible = isModified,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LocalContentColor.current)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onTabClose(tab.id) },
                            contentAlignment = Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = strings.closeTabDescription(tab.title),
                                modifier = Modifier.size(14.dp),
                                tint = LocalContentColor.current
                            )
                        }
                    }

                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(
                            extraSmall = RoundedCornerShape(16.dp)
                        )
                    ) {
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        strings.closeTabAction,
                                        fontFamily = uiFontFamily(),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = { onTabClose(tab.id); isMenuExpanded = false },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = null
                                    )
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        strings.closeOthers,
                                        fontFamily = uiFontFamily(),
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = { onTabCloseOthers(tab.id); isMenuExpanded = false },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.CloseFullscreen,
                                        contentDescription = null
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        strings.closeAllTabs,
                                        fontFamily = uiFontFamily(),
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = { onTabCloseAll(); isMenuExpanded = false },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.DeleteSweep,
                                        contentDescription = null
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
