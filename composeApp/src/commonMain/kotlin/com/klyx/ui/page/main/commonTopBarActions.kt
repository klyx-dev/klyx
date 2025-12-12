package com.klyx.ui.page.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.klyx.LocalNavigator
import com.klyx.Route
import com.klyx.core.LocalNotifier
import com.klyx.core.PlatformContext
import com.klyx.core.file.Project
import com.klyx.core.file.isKlyxTempFile
import com.klyx.core.file.toKxFile
import com.klyx.core.util.value
import com.klyx.di.LocalEditorViewModel
import com.klyx.res.Res.string
import com.klyx.res.notification_no_active_file
import com.klyx.res.notification_saved
import com.klyx.res.settings
import com.klyx.runner.CodeRunner
import com.klyx.tab.FileTab
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@RememberInComposition
fun commonTopBarActions(project: Project) = movableContentWithReceiverOf<RowScope> {
    val editorViewModel = LocalEditorViewModel.current

    val navigator = LocalNavigator.current
    val notifier = LocalNotifier.current

    val activeTab by editorViewModel.activeTab.collectAsState()
    val activeFile by editorViewModel.activeFile.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    val fileSaver = rememberFileSaverLauncher { file ->
        if (file != null) {
            val saved = editorViewModel.saveCurrentAs(file.toKxFile())
            if (saved) notifier.toast(com.klyx.core.util.string(string.notification_saved))
        }
    }

    val canUndo by editorViewModel.canUndo.collectAsState()
    val canRedo by editorViewModel.canRedo.collectAsState()

    activeTab?.let { tab ->
        if (tab is FileTab && !tab.isReadOnly) {
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

            val runner = remember { CodeRunner() }

            if (runner.canRun(tab.file)) {
                TopBarIconButton(
                    Icons.Outlined.PlayArrow,
                    contentDescription = "Run",
                    onClick = {
                        coroutineScope.launch {
                            runner.run(tab.file)
                        }
                    }
                )
            }

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

    if (activeTab == null || activeTab !is FileTab) {
        TopBarIconButton(
            Icons.Outlined.Settings,
            contentDescription = stringResource(string.settings),
            onClick = { navigator.navigateTo(Route.Settings) }
        )
    }

    OverflowMenu(
        project = project,
        onNavigateTo = navigator::navigateTo
    )
}

@Composable
private fun OverflowMenu(
    project: Project,
    onNavigateTo: (NavKey) -> Unit
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
                onShowFileMenu = {
                    expanded = false
                    showFileMenu = !showFileMenu
                },
                onShowHelpMenu = {
                    expanded = false
                    showHelpMenu = !showHelpMenu
                },
                onNavigateTo = onNavigateTo
            ) { expanded = false }
        }

        FileMenu(
            expanded = showFileMenu,
            onDismissRequest = { showFileMenu = false },
        )

        HelpMenu(
            expanded = showHelpMenu,
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

expect fun PlatformContext.openTerminal()
