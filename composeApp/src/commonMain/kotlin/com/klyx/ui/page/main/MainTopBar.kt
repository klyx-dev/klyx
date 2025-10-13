package com.klyx.ui.page.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.LocalDrawerState
import com.klyx.core.FpsTracker
import com.klyx.core.LocalAppSettings
import com.klyx.core.LocalNotifier
import com.klyx.core.file.toKxFile
import com.klyx.core.toFixed
import com.klyx.core.ui.Route
import com.klyx.extension.api.Project
import com.klyx.filetree.FileTreeViewModel
import com.klyx.res.Res
import com.klyx.res.Res.string
import com.klyx.res.label_fps
import com.klyx.res.notification_no_active_file
import com.klyx.res.notification_saved
import com.klyx.res.settings
import com.klyx.tab.Tab
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    isTabOpen: Boolean,
    activeTab: Tab?,
    project: Project,
    editorViewModel: EditorViewModel,
    klyxViewModel: KlyxViewModel,
    fileTreeViewModel: FileTreeViewModel,
    onNavigateToRoute: (String) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    val notifier = LocalNotifier.current
    val activeFile by editorViewModel.activeFile.collectAsStateWithLifecycle()

    val fileSaver = rememberFileSaverLauncher { file ->
        if (file != null) {
            val saved = editorViewModel.saveCurrentAs(file.toKxFile())
            if (saved) notifier.toast(com.klyx.core.string(string.notification_saved))
        }
    }

    val commonActions: @Composable RowScope.() -> Unit = {
        SettingsButton(onNavigateToRoute)

        activeTab?.let { tab ->
            if (isTabOpen && tab is Tab.FileTab && !tab.isInternal) {
                IconButton(
                    onClick = {
                        val file = activeFile
                        if (file == null) {
                            notifier.notify(com.klyx.core.string(string.notification_no_active_file))
                            return@IconButton
                        }

                        if (file.path == "/untitled") {
                            fileSaver.launch(file.name)
                        } else {
                            val saved = editorViewModel.saveCurrent()
                            if (saved) notifier.toast(com.klyx.core.string(string.notification_saved))
                        }
                    },
                    enabled = tab.isModified
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = "Save")
                }
            }
        }

        OverflowMenu(project, editorViewModel, klyxViewModel, fileTreeViewModel)
    }

    val fpsTracker = remember { FpsTracker() }
    val fps by fpsTracker.fps

    LaunchedEffect(Unit) {
        launch(Dispatchers.Default) {
            fpsTracker.start()
        }
    }

    if (isTabOpen) {
        TopAppBar(
            title = {
                if (LocalAppSettings.current.showFps) {
                    Text(
                        text = stringResource(Res.string.label_fps, fps.toFixed(1)),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            navigationIcon = { FileTreeButton() },
            actions = commonActions
        )
    } else {
        LargeTopAppBar(
            title = {
                Row {
                    Text("Klyx")

                    if (LocalAppSettings.current.showFps) {
                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = stringResource(Res.string.label_fps, fps.toFixed(1)),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Bottom)
                        )
                    }
                }
            },
            navigationIcon = { FileTreeButton() },
            scrollBehavior = scrollBehavior,
            actions = commonActions
        )
    }
}

@Composable
private fun OverflowMenu(
    project: Project,
    editorViewModel: EditorViewModel,
    klyxViewModel: KlyxViewModel,
    fileTreeViewModel: FileTreeViewModel
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var showFileMenu by rememberSaveable { mutableStateOf(false) }
    var showHelpMenu by rememberSaveable { mutableStateOf(false) }
    var showKlyxMenu by rememberSaveable { mutableStateOf(false) }

    Box(modifier = Modifier.wrapContentSize(Alignment.TopEnd)) {
        IconButton(onClick = { expanded = !expanded }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = "Menu")
        }

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
                onShowKlyxMenu = {
                    expanded = false
                    showKlyxMenu = !showKlyxMenu
                },
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
            onDismissRequest = { showHelpMenu = false }
        )

        KlyxMenu(
            expanded = showKlyxMenu,
            onDismissRequest = { showKlyxMenu = !showKlyxMenu },
            editorViewModel = editorViewModel,
            klyxViewModel = klyxViewModel
        )
    }
}

@Composable
private fun FileTreeButton() {
    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    IconButton(onClick = { scope.launch { drawerState.open() } }) {
        Icon(
            Icons.Rounded.Menu,
            contentDescription = "Open file tree"
        )
    }
}

@Composable
private fun SettingsButton(onNavigate: (String) -> Unit) {
    IconButton(
        onClick = { onNavigate(Route.SETTINGS_PAGE) }
    ) {
        Icon(
            Icons.Outlined.Settings,
            contentDescription = stringResource(Res.string.settings)
        )
    }
}
