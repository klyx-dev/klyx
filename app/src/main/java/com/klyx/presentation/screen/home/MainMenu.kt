package com.klyx.presentation.screen.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.klyx.R
import com.klyx.api.data.editor.EditorAction
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.ui.ToolbarAction
import com.klyx.api.ui.ToolbarCategory
import com.klyx.i18n.strings
import com.klyx.presentation.components.ExpressiveMenuItem
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.ui.theme.uiFontFamily

sealed interface MenuAction {
    data object Share : MenuAction
}

@Composable
fun MainMenu(
    activeTab: WorkspaceTab?,
    toolbarActions: List<ToolbarAction>,
    onAction: (MenuAction) -> Unit,
    onEditorAction: (EditorAction) -> Unit,
    onSaveAsClick: () -> Unit
) {
    val navigator = LocalNavigator.current
    var showMenu by remember { mutableStateOf(false) }

    val byCategory = toolbarActions.groupBy { it.category }
    val currentFileActions = byCategory[ToolbarCategory.CurrentFile].orEmpty().sortedBy { it.priority }
    val workspaceActions = byCategory[ToolbarCategory.Workspace].orEmpty().sortedBy { it.priority }
    val customCategories = byCategory.keys
        .minus(setOf(ToolbarCategory.CurrentFile, ToolbarCategory.Workspace, ToolbarCategory.Plugins))
        .sorted()
    val pluginsActions = byCategory[ToolbarCategory.Plugins].orEmpty().sortedBy { it.priority }
    val hasCustomOrPlugins = customCategories.isNotEmpty() || pluginsActions.isNotEmpty()

    Box {
        FilledIconButton(
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = { showMenu = true }
        ) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = strings.moreOptions
            )
        }

        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(20.dp)
            )
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.widthIn(min = 180.dp)
            ) {
                if (activeTab is WorkspaceTab.TextFile || activeTab is WorkspaceTab.ImageFile) {
                    MenuSectionHeader(strings.currentFileSection)

                    if (activeTab is WorkspaceTab.TextFile) {
                        ExpressiveMenuItem(
                            text = strings.saveAs,
                            icon = painterResource(R.drawable.save_as_24px),
                            onClick = {
                                showMenu = false
                                onSaveAsClick()
                            }
                        )
                    }

                    ExpressiveMenuItem(
                        text = strings.share,
                        icon = painterResource(R.drawable.share_24px),
                        onClick = {
                            showMenu = false
                            onAction(Share)
                        }
                    )

                    MenuActionItems(
                        actions = currentFileActions,
                        onDismiss = { showMenu = false }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                MenuSectionHeader(strings.workspaceSection)

                ExpressiveMenuItem(
                    text = strings.terminal,
                    icon = painterResource(R.drawable.terminal_2_24px),
                    onClick = {
                        showMenu = false
                        navigator.navigateTo(Terminal)
                    }
                )

                ExpressiveMenuItem(
                    text = strings.settings,
                    icon = painterResource(R.drawable.settings_24px),
                    onClick = {
                        showMenu = false
                        navigator.navigateTo(Settings)
                    }
                )

                MenuActionItems(
                    actions = workspaceActions,
                    onDismiss = { showMenu = false }
                )

                if (hasCustomOrPlugins) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                customCategories.forEach { category ->
                    MenuSectionHeader(category.name)
                    MenuActionItems(
                        actions = byCategory[category]!!.sortedBy { it.priority },
                        onDismiss = { showMenu = false }
                    )
                }

                if (pluginsActions.isNotEmpty()) {
                    if (customCategories.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    MenuSectionHeader(strings.plugins)

                    MenuActionItems(
                        actions = pluginsActions,
                        onDismiss = { showMenu = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        fontFamily = uiFontFamily(),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun MenuActionItems(actions: List<ToolbarAction>, onDismiss: () -> Unit) {
    actions.forEach { action ->
        ExpressiveMenuItem(
            text = action.label,
            icon = action.icon,
            onClick = {
                onDismiss()
                action.onClick()
            }
        )
    }
}
