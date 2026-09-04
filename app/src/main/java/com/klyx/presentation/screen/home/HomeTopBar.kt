package com.klyx.presentation.screen.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.klyx.R
import com.klyx.api.data.editor.EditorAction
import com.klyx.api.data.editor.Save
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.ui.ToolbarAction
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.api.util.thenIf
import com.klyx.app.icons.Save
import com.klyx.i18n.strings
import com.klyx.presentation.navigation.LocalNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    drawerState: DrawerState,
    scope: CoroutineScope,
    openTabs: ImmutableList<WorkspaceTab>,
    activeTab: WorkspaceTab?,
    toolbarActions: List<ToolbarAction>,
    runnable: Boolean,
    onRun: (() -> Unit)?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabCloseOthers: (String) -> Unit,
    onTabCloseAll: () -> Unit,
    onAction: (EditorAction) -> Unit,
    onMenuAction: (MenuAction) -> Unit,
    onSaveAsClick: () -> Unit
) {
    val gradientColors = persistentListOf(
        MaterialTheme.colorScheme.primaryContainer,
        Color.Transparent
    )

    val brush = remember(gradientColors) {
        Brush.verticalGradient(colors = gradientColors)
    }

    val isAmoledDarkMode = LocalAppSettings.current.appearance.amoledDarkMode && LocalIsDarkMode.current

    Column(
        modifier = Modifier
            .thenIf(!isAmoledDarkMode) {
                background(brush)
            }
    ) {
        TopAppBar(
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Transparent,
                scrolledContainerColor = Transparent
            ),
            title = {},
            navigationIcon = {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    shapes = IconButtonDefaults.shapes(
                        shape = MaterialTheme.shapes.medium,
                        pressedShape = MaterialTheme.shapes.small
                    ),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.folder_code_24px),
                        contentDescription = strings.fileExplorer
                    )
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val appearanceSettings = LocalAppSettings.current.appearance

                    if (activeTab is WorkspaceTab.TextFile) {
                        FilledIconButton(
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = { onAction(Save(activeTab.file)) }
                        ) {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = strings.saveFile
                            )
                        }
                    }

                    if (runnable && onRun != null) {
                        FilledIconButton(
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            onClick = onRun
                        ) {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = strings.runFile
                            )
                        }
                    }

                    if (appearanceSettings.showTerminalInTopbar) {
                        val navigator = LocalNavigator.current
                        FilledIconButton(
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = { navigator.navigateTo(Terminal) }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.terminal_2_24px),
                                contentDescription = strings.terminal
                            )
                        }
                    }

                    MainMenu(
                        activeTab = activeTab,
                        toolbarActions = toolbarActions,
                        onAction = onMenuAction,
                        onEditorAction = onAction,
                        onSaveAsClick = onSaveAsClick
                    )
                }
            }
        )

        if (openTabs.isNotEmpty()) {
            EditorTabs(
                openTabs = openTabs,
                activeTab = activeTab,
                onTabClick = onTabClick,
                onTabClose = onTabClose,
                onTabCloseOthers = onTabCloseOthers,
                onTabCloseAll = onTabCloseAll
            )
        }
    }
}
