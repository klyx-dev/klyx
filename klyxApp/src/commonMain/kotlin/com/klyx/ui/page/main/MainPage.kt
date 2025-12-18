package com.klyx.ui.page.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.animateFloatingActionButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.LocalLogBuffer
import com.klyx.LocalWindowSizeClass
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.file.KxFile
import com.klyx.core.file.Project
import com.klyx.core.file.Worktree
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.file.toKxFile
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.language
import com.klyx.core.noLocalProvidedFor
import com.klyx.core.registerGeneralCommands
import com.klyx.core.theme.ThemeManager
import com.klyx.core.ui.component.TextButtonWithShortcut
import com.klyx.di.LocalEditorViewModel
import com.klyx.di.LocalKlyxViewModel
import com.klyx.di.LocalStatusBarViewModel
import com.klyx.editor.ComposeEditorState
import com.klyx.editor.CursorState
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.editor.SoraEditorState
import com.klyx.filetree.FileTree
import com.klyx.filetree.FileTreeNode
import com.klyx.filetree.toFileTreeNodes
import com.klyx.isWidthAtLeastMediumOrExpanded
import com.klyx.resources.Res
import com.klyx.resources.open_a_project
import com.klyx.tab.FileTab
import com.klyx.ui.component.ThemeSelector
import com.klyx.ui.component.cmd.CommandPalette
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.component.log.LogViewerSheet
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.openLogViewer
import io.github.vinceglb.filekit.dialogs.compose.PickerResultLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalCodeEditorApi::class)
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

    val scope = rememberCoroutineScope()

    WorkspaceDrawer(editorVM = editorViewModel, klyxVM = klyxViewModel) {
        val drawerState = LocalDrawerState.current

        val openDrawerIfClosed = @IgnorableReturnValue {
            scope.launch {
                if (drawerState.isClosed) drawerState.open()
            }
        }

        var fabMenuExpanded by rememberSaveable { mutableStateOf(false) }

        registerGeneralCommands()

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
                            when (val state = tab.editorState) {
                                is ComposeEditorState -> {
                                    with(state.state) {
                                        cursor.collectLatest { cursor ->
                                            statusBarViewModel.setCursorState(cursor.let {
                                                CursorState(
                                                    left = if (selection.collapsed) it.line else selection.min,
                                                    right = if (selection.collapsed) it.line else selection.max,
                                                    leftLine = if (selection.collapsed) it.line else cursorAt(selection.min).line,
                                                    rightLine = if (selection.collapsed) it.line else cursorAt(selection.max).line,
                                                    leftColumn = if (selection.collapsed) it.column else cursorAt(
                                                        selection.min
                                                    ).column,
                                                    rightColumn = if (selection.collapsed) it.column else cursorAt(
                                                        selection.max
                                                    ).column,
                                                    isSelected = !selection.collapsed
                                                )
                                            })
                                        }
                                    }
                                }

                                is SoraEditorState -> {
                                    state.state.cursor.collectLatest { cursorState ->
                                        statusBarViewModel.setCursorState(cursorState)
                                    }
                                }
                            }
                        }

                        else -> {}
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
}

val LocalDrawerState = staticCompositionLocalOf<DrawerState> {
    noLocalProvidedFor("LocalDrawerState")
}

val worktreeDrawerWidth: Dp
    @ReadOnlyComposable
    @Composable
    get() {
        val windowDpSize = LocalWindowInfo.current.containerDpSize
        val windowSizeClass = LocalWindowSizeClass.current
        return when {
            windowSizeClass.isWidthAtLeastMediumOrExpanded -> windowDpSize.width * 0.35f
            else -> windowDpSize.width * 0.7f
        }
    }

@Composable
private fun WorkspaceDrawer(
    editorVM: EditorViewModel,
    klyxVM: KlyxViewModel,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val drawerModifier = Modifier
        .fillMaxHeight()
        .imePadding()
        .navigationBarsPadding()
    val scope = rememberCoroutineScope()

    val project by klyxVM.openedProject.collectAsState()
    val isFileTabOpen by editorVM.isTabOpen { it is FileTab }.collectAsState()

    CompositionLocalProvider(LocalDrawerState provides drawerState) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen || !isFileTabOpen,
            drawerContent = {
                ModalDrawerSheet(
                    drawerState = drawerState,
                    drawerContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                    modifier = modifier.width(worktreeDrawerWidth).then(drawerModifier)
                ) {
                    val directoryPicker = rememberDirectoryPickerLauncher { file ->
                        if (file != null) {
                            val directory = file.toKxFile()
                            if (directory.isPermissionRequired(R_OK or W_OK)) {
                                klyxVM.showPermissionDialog()
                            } else {
                                klyxVM.openProject(Worktree(directory))
                                scope.launch {
                                    if (drawerState.isClosed) {
                                        drawerState.open()
                                    }
                                }
                            }
                        }
                    }

                    DrawerContent(
                        project = project,
                        directoryPicker = directoryPicker,
                        onFileClick = editorVM::openFile,
                        scope = scope,
                        onDismissRequest = drawerState::close
                    )
                }
            },
            content = content
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DrawerContent(
    project: Project,
    directoryPicker: PickerResultLauncher,
    onFileClick: (KxFile, Worktree) -> Unit,
    scope: CoroutineScope,
    onDismissRequest: suspend () -> Unit
) {
    if (project.isEmpty()) {
        DirectoryPickerButton(directoryPicker)
        return
    }

    val nodes by produceState<Map<Worktree, FileTreeNode>?>(null) {
        value = project.toFileTreeNodes()
    }

    when (val tree = nodes) {
        null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                ContainedLoadingIndicator()
            }
        }

        else -> {
            FileTree(
                rootNodes = tree,
                modifier = Modifier.fillMaxSize(),
                onFileClick = { file, worktree ->
                    onFileClick(file, worktree)
                    scope.launch { onDismissRequest() }
                }
            )
        }
    }
}

@Composable
private fun DirectoryPickerButton(directoryPicker: PickerResultLauncher) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.statusBars),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButtonWithShortcut(
            text = stringResource(Res.string.open_a_project),
            modifier = Modifier.padding(top = 20.dp),
            shortcut = keyShortcutOf(ctrl = true, key = Key.O)
        ) {
            directoryPicker.launch()
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
