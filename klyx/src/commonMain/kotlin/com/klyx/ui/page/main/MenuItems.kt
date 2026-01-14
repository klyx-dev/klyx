package com.klyx.ui.page.main

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import com.klyx.Route
import com.klyx.core.LocalPlatformContext
import com.klyx.core.PlatformContext
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.icon.BackToTab
import com.klyx.core.icon.KlyxIcons
import com.klyx.core.icon.Pip
import com.klyx.core.ui.component.DropdownMenuDivider
import com.klyx.core.ui.component.DropdownMenuItem
import com.klyx.di.LocalEditorViewModel
import com.klyx.di.LocalKlyxViewModel
import com.klyx.icons.Cancel
import com.klyx.icons.ChevronRight
import com.klyx.icons.Close
import com.klyx.icons.ExitToApp
import com.klyx.icons.Extension
import com.klyx.icons.Folder
import com.klyx.icons.Help
import com.klyx.icons.Icons
import com.klyx.icons.KeyboardCommandKey
import com.klyx.icons.Settings
import com.klyx.icons.Terminal
import com.klyx.project.Project
import com.klyx.resources.Res
import com.klyx.resources.settings
import com.klyx.viewmodel.openExtensionScreen
import org.jetbrains.compose.resources.stringResource

@Suppress("UnusedReceiverParameter")
@Composable
fun ColumnScope.DropdownMenuItems(
    project: Project,
    onShowFileMenu: () -> Unit,
    onShowHelpMenu: () -> Unit,
    onNavigateTo: (NavKey) -> Unit,
    onDismissRequest: () -> Unit = {}
) {
    val editorViewModel = LocalEditorViewModel.current
    val klyxViewModel = LocalKlyxViewModel.current

    val context = LocalPlatformContext.current
    val isTabOpen by editorViewModel.isTabOpen.collectAsStateWithLifecycle()

    DropdownMenuItem(
        text = "New Window",
        onClick = { openNewWindow(context) },
        icon = {
            Icon(
                KlyxIcons.BackToTab,
                contentDescription = "Open New Window"
            )
        },
        shortcut = keyShortcutOf(ctrl = true, shift = true, key = Key.N)
    )

    DropdownMenuItem(
        text = { Text("File") },
        onClick = onShowFileMenu,
        leadingIcon = {
            Icon(
                Icons.Folder,
                contentDescription = "File Actions"
            )
        },
        trailingIcon = {
            Icon(
                Icons.ChevronRight,
                contentDescription = null
            )
        }
    )

    DropdownMenuDivider()

    DropdownMenuItem(
        text = { Text("Terminal") },
        onClick = {
            onDismissRequest()
            //onNavigateTo(Route.Terminal)
            context.openTerminal()
        },
        leadingIcon = {
            Icon(
                Icons.Terminal,
                contentDescription = "Terminal"
            )
        }
    )

    if (isTabOpen) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.settings)) },
            onClick = {
                onDismissRequest()
                onNavigateTo(Route.Settings)
            },
            leadingIcon = {
                Icon(
                    Icons.Settings,
                    contentDescription = stringResource(Res.string.settings)
                )
            }
        )
    }

    DropdownMenuDivider()

    DropdownMenuItem(
        text = "Command Palette",
        onClick = {
            CommandManager.showCommandPalette()
            onDismissRequest()
        },
        icon = {
            Icon(
                Icons.KeyboardCommandKey,
                contentDescription = "Open Command Palette"
            )
        },
        shortcut = keyShortcutOf(ctrl = true, shift = true, key = Key.P)
    )

    DropdownMenuItem(
        text = "Extensions",
        onClick = {
            editorViewModel.openExtensionScreen()
            onDismissRequest()
        },
        icon = {
            Icon(
                Icons.Extension,
                contentDescription = "Extensions"
            )
        },
        shortcut = keyShortcutOf(ctrl = true, shift = true, key = Key.X)
    )

    DropdownMenuDivider()

    if (project.isNotEmpty()) {
        DropdownMenuItem(
            text = { Text("Close Project") },
            onClick = {
                klyxViewModel.closeProject()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    Icons.Cancel,
                    contentDescription = "Close Project"
                )
            }
        )
    }

    if (isTabOpen) {
        DropdownMenuItem(
            text = { Text("Close Editor") },
            onClick = {
                editorViewModel.closeAllTabs()
                onDismissRequest()
            },
            leadingIcon = {
                Icon(
                    Icons.Close,
                    contentDescription = "Close Editor"
                )
            }
        )
    }

    DropdownMenuItem(
        text = "Close Window",
        onClick = { closeCurrentWindow(context) },
        icon = {
            Icon(
                KlyxIcons.Pip,
                contentDescription = "Close Window"
            )
        },
        shortcut = keyShortcutOf(ctrl = true, shift = true, key = Key.W)
    )

    DropdownMenuDivider()

    DropdownMenuItem(
        text = { Text("Help") },
        onClick = onShowHelpMenu,
        leadingIcon = {
            Icon(
                Icons.Help,
                contentDescription = "Help"
            )
        },
        trailingIcon = {
            Icon(
                Icons.ChevronRight,
                contentDescription = null
            )
        }
    )

    DropdownMenuItem(
        text = "Quit",
        onClick = { quitApp() },
        icon = {
            Icon(
                Icons.ExitToApp,
                contentDescription = "Quit App"
            )
        },
        shortcut = keyShortcutOf(key = Key.Q, ctrl = true)
    )

    DropdownMenuDivider()
}

internal expect fun openNewWindow(context: PlatformContext)
internal expect fun closeCurrentWindow(context: PlatformContext)

internal expect fun quitApp(): Nothing
