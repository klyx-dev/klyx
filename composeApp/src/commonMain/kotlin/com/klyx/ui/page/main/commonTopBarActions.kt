package com.klyx.ui.page.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.annotation.RememberInComposition
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentWithReceiverOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.klyx.AppRoute
import com.klyx.core.LocalNotifier
import com.klyx.core.file.isKlyxTempFile
import com.klyx.core.file.toKxFile
import com.klyx.core.value
import com.klyx.extension.api.Project
import com.klyx.filetree.FileTreeViewModel
import com.klyx.res.Res
import com.klyx.res.Res.string
import com.klyx.res.notification_no_active_file
import com.klyx.res.notification_saved
import com.klyx.res.settings
import com.klyx.tab.Tab
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import org.jetbrains.compose.resources.stringResource

@RememberInComposition
fun commonTopBarActions(
    project: Project,
    editorViewModel: EditorViewModel,
    klyxViewModel: KlyxViewModel,
    fileTreeViewModel: FileTreeViewModel,
    onNavigateToRoute: (AppRoute) -> Unit,
) = movableContentWithReceiverOf<RowScope> {
    val notifier = LocalNotifier.current

    val activeTab by editorViewModel.activeTab.collectAsState()
    val activeFile by editorViewModel.activeFile.collectAsState()

    val fileSaver = rememberFileSaverLauncher { file ->
        if (file != null) {
            val saved = editorViewModel.saveCurrentAs(file.toKxFile())
            if (saved) notifier.toast(com.klyx.core.string(string.notification_saved))
        }
    }

    val canUndo by editorViewModel.canUndo.collectAsState()
    val canRedo by editorViewModel.canRedo.collectAsState()

    activeTab?.let { tab ->
        if (tab is Tab.FileTab && !tab.isReadOnly) {
            TopBarIconButton(
                Icons.AutoMirrored.Filled.Undo,
                enabled = canUndo,
                contentDescription = "Undo",
                onClick = editorViewModel::undo
            )

            TopBarIconButton(
                Icons.AutoMirrored.Filled.Redo,
                enabled = canRedo,
                contentDescription = "Redo",
                onClick = editorViewModel::redo
            )

            TopBarIconButton(
                Icons.Outlined.Save,
                contentDescription = "Save",
                enabled = tab.isModified || tab.file.isKlyxTempFile(),
                onClick = {
                    val file = activeFile
                    if (file == null) {
                        notifier.notify(string.notification_no_active_file.value)
                        return@TopBarIconButton
                    }

                    if (file.path == "/untitled") {
                        fileSaver.launch(file.name)
                    } else {
                        val saved = editorViewModel.saveCurrent()
                        if (saved) notifier.toast(string.notification_saved.value)
                    }
                }
            )
        }
    }

    if (activeTab == null) {
        TopBarIconButton(
            Icons.Outlined.Settings,
            contentDescription = stringResource(Res.string.settings),
            onClick = { onNavigateToRoute(AppRoute.Settings.SettingsPage) }
        )
    }

    OverflowMenu(
        project = project,
        editorViewModel = editorViewModel,
        klyxViewModel = klyxViewModel,
        fileTreeViewModel = fileTreeViewModel,
        onNavigateToRoute = onNavigateToRoute
    )
}

@Composable
private fun OverflowMenu(
    project: Project,
    editorViewModel: EditorViewModel,
    klyxViewModel: KlyxViewModel,
    fileTreeViewModel: FileTreeViewModel,
    onNavigateToRoute: (AppRoute) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showFileMenu by rememberSaveable { mutableStateOf(false) }
    var showHelpMenu by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        TopBarIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = "Menu",
            onClick = { expanded = !expanded }
        )

        DropdownMenu(
            expanded = expanded,
            offset = DpOffset((-5).dp, 0.dp),
            shape = MaterialTheme.shapes.medium,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItems(
                project = project,
                editorViewModel = editorViewModel,
                klyxViewModel = klyxViewModel,
                onShowFileMenu = {
                    expanded = false
                    showFileMenu = !showFileMenu
                },
                onShowHelpMenu = {
                    expanded = false
                    showHelpMenu = !showHelpMenu
                },
                onNavigateToRoute = onNavigateToRoute,
                onDismissRequest = { expanded = false }
            )
        }

        FileMenu(
            expanded = showFileMenu,
            onDismissRequest = { showFileMenu = false },
            editorViewModel = editorViewModel,
            klyxViewModel = klyxViewModel,
            fileTreeViewModel = fileTreeViewModel
        )

        HelpMenu(
            expanded = showHelpMenu,
            klyxViewModel = klyxViewModel,
            editorViewModel = editorViewModel,
            onDismissRequest = { showHelpMenu = false }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    enabled: Boolean = true,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    IconButton(enabled = enabled, onClick = onClick, shapes = IconButtonDefaults.shapes()) {
        Icon(icon, contentDescription)
    }
}
