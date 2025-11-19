package com.klyx.ui.page.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.waterfall
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.LocalDrawerState
import com.klyx.LocalLogBuffer
import com.klyx.openIfClosed
import com.klyx.core.cmd.CommandManager
import com.klyx.core.file.KxFile
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.file.toKxFile
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.language
import com.klyx.core.theme.ThemeManager
import com.klyx.di.LocalEditorViewModel
import com.klyx.di.LocalKlyxViewModel
import com.klyx.di.LocalStatusBarViewModel
import com.klyx.extension.api.Worktree
import com.klyx.tab.FileTab
import com.klyx.ui.component.ThemeSelector
import com.klyx.ui.component.cmd.CommandPalette
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.component.log.LogViewerSheet
import com.klyx.viewmodel.openLogViewer
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainPage(modifier: Modifier = Modifier) {
    val editorViewModel = LocalEditorViewModel.current
    val klyxViewModel = LocalKlyxViewModel.current
    val statusBarViewModel = LocalStatusBarViewModel.current

    val logBuffer = LocalLogBuffer.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val appState by klyxViewModel.appState.collectAsStateWithLifecycle()
    val project by klyxViewModel.openedProject.collectAsStateWithLifecycle()
    val isTabOpen by editorViewModel.isTabOpen.collectAsStateWithLifecycle()
    val activeTab by editorViewModel.activeTab.collectAsStateWithLifecycle()

    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    val openDrawerIfClosed = {
        scope.launch { drawerState.openIfClosed() }
    }

    var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MainTopBar(
                isTabOpen = isTabOpen,
                activeTab = activeTab,
                project = project,
                scrollBehavior = scrollBehavior
            )
        },
        floatingActionButton = {
            val directoryPicker = rememberDirectoryPickerLauncher { file ->
                if (file != null) {
                    val kx = file.toKxFile()

                    if (kx.isPermissionRequired(R_OK or W_OK)) {
                        klyxViewModel.showPermissionDialog()
                    } else {
                        klyxViewModel.openProject(Worktree(kx))
                        openDrawerIfClosed()
                    }
                }
            }

            val filePicker = rememberFilePickerLauncher { file ->
                if (file != null) {
                    editorViewModel.openFile(file.toKxFile())
                }
            }

            FloatingActionButtonMenu(
                expanded = fabMenuExpanded && !isTabOpen,
                button = {
                    ToggleFloatingActionButton(
                        modifier = Modifier
                            .semantics {
                                traversalIndex = -1f
                                stateDescription = if (fabMenuExpanded) "Expanded" else "Collapsed"
                                contentDescription = "Toggle menu"
                            }.animateFloatingActionButton(
                                visible = !isTabOpen,
                                alignment = Alignment.BottomEnd
                            ),
                        checked = fabMenuExpanded,
                        onCheckedChange = { fabMenuExpanded = !fabMenuExpanded },
                        containerSize = ToggleFloatingActionButtonDefaults.containerSizeMedium(),
                        containerCornerRadius = ToggleFloatingActionButtonDefaults.containerCornerRadiusMedium()
                    ) {
                        val imageVector by remember {
                            derivedStateOf {
                                if (checkedProgress > 0.5f) Icons.Filled.Close else Icons.Filled.Add
                            }
                        }

                        Icon(
                            painter = rememberVectorPainter(imageVector),
                            contentDescription = null,
                            modifier = Modifier.animateIcon(
                                checkedProgress = { checkedProgress },
                                size = ToggleFloatingActionButtonDefaults.iconSizeMedium()
                            ),
                        )
                    }
                }
            ) {
                FloatingActionButtonMenuItem(
                    onClick = {
                        editorViewModel.openFile(KxFile("untitled"))
                        fabMenuExpanded = false
                    },
                    text = { Text("New File") },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = "New File") }
                )

                FloatingActionButtonMenuItem(
                    onClick = {
                        filePicker.launch()
                        fabMenuExpanded = false
                    },
                    text = { Text("Open File") },
                    icon = { Icon(Icons.Outlined.FileOpen, contentDescription = "Open File") }
                )

                FloatingActionButtonMenuItem(
                    onClick = {
                        directoryPicker.launch()
                        fabMenuExpanded = false
                    },
                    text = { Text("Open Folder") },
                    icon = { Icon(Icons.Outlined.DriveFolderUpload, contentDescription = "Open Folder") }
                )
            }
        }
    ) { padding ->

        Column(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
            LaunchedEffect(Unit) {
                launch {
                    editorViewModel.activeFile.collect { file ->
                        statusBarViewModel.setLanguage(file?.language())
                    }
                }
            }

            val activeTab by editorViewModel.activeTab.collectAsState(null)

            LaunchedEffect(activeTab?.id) {
                when (val tab = activeTab) {
                    is FileTab -> {
//                        tab.editorState.cursor.collectLatest { cursorState ->
//                            statusBarViewModel.setCursorState(cursorState)
//                        }
                    }

                    else -> {
                        statusBarViewModel.setCursorState(null)
                    }
                }
            }

            EditorScreen(modifier = Modifier.weight(1f))

            if (appState.showLogViewer) {
                LogViewerSheet(
                    buffer = logBuffer,
                    onOpenAsTabClick = {
                        editorViewModel.openLogViewer()
                    },
                    onDismissRequest = klyxViewModel::dismissLogViewer
                )
            }

            MenuStateDialogs()

            if (CommandManager.showCommandPalette) {
                CommandPalette(
                    commands = CommandManager.commands,
                    recentlyUsedCommands = CommandManager.recentlyUsedCommands,
                    onDismissRequest = CommandManager::hideCommandPalette
                )
            }

            if (ThemeManager.showThemeSelector) {
                ThemeSelector(
                    onDismissRequest = ThemeManager::hideThemeSelector
                )
            }
        }
    }
}

@Composable
private fun MenuStateDialogs() {
    val klyxViewModel = LocalKlyxViewModel.current
    val menuState by klyxViewModel.klyxMenuState.collectAsState()

    if (menuState.showInfoDialog) {
        InfoDialog(onDismissRequest = { klyxViewModel.dismissInfoDialog() })
    }

    if (menuState.showGiveFeedbackDialog) {
        GiveFeedbackDialog(onDismissRequest = { klyxViewModel.dismissGiveFeedbackDialog() })
    }
}
