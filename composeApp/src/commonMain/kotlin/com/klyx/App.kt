@file:OptIn(ExperimentalCodeEditorApi::class)

package com.klyx

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.core.LocalNotifier
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.components.TextButtonWithShortcut
import com.klyx.core.event.subscribeToEvent
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.file.toKxFile
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.language
import com.klyx.core.logging.KxLog
import com.klyx.core.noLocalProvidedFor
import com.klyx.core.notification.ui.NotificationOverlay
import com.klyx.core.theme.ThemeManager
import com.klyx.editor.ExperimentalCodeEditorApi
import com.klyx.extension.api.Worktree
import com.klyx.filetree.FileTree
import com.klyx.filetree.toFileTreeNodes
import com.klyx.platform.PlatformInfo
import com.klyx.ui.component.ThemeSelector
import com.klyx.ui.component.cmd.CommandPalette
import com.klyx.ui.component.editor.EditorScreen
import com.klyx.ui.component.editor.PermissionDialog
import com.klyx.ui.component.editor.StatusBar
import com.klyx.ui.component.editor.StatusBarState
import com.klyx.ui.component.log.LogBuffer
import com.klyx.ui.component.log.LogViewerSheet
import com.klyx.ui.component.menu.MainMenuBar
import com.klyx.ui.theme.KlyxTheme
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.openLogViewer
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.viewmodel.koinViewModel

val LocalDrawerState = staticCompositionLocalOf<DrawerState> {
    noLocalProvidedFor<DrawerState>()
}

val DrawerWidth: Dp
    @ReadOnlyComposable
    @Composable
    get() {
        val density = LocalDensity.current
        val windowInfo = LocalWindowInfo.current
        val width = windowInfo.containerSize.width * if (PlatformInfo.isMobile) 0.75f else 0.3f
        return with(density) { width.toDp() }
    }

@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
@Composable
fun App(
    dynamicColor: Boolean = true,
    darkTheme: Boolean = false,
    useThemeExtension: Boolean = true,
    themeName: String? = null,
    extraContent: @Composable () -> Unit = {}
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val notifier = LocalNotifier.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val editorViewModel = koinViewModel<EditorViewModel>()
    val klyxViewModel = koinViewModel<KlyxViewModel>()

    val appState by klyxViewModel.appState.collectAsStateWithLifecycle()

    val project by klyxViewModel.openedProject.collectAsState()

    val directoryPicker = rememberDirectoryPickerLauncher { file ->
        if (file != null) {
            val kx = file.toKxFile()

            if (kx.isPermissionRequired(R_OK or W_OK)) {
                klyxViewModel.showPermissionDialog()
            } else {
                klyxViewModel.openProject(Worktree(kx))
                if (drawerState.isClosed) {
                    scope.launch { drawerState.open() }
                }
            }
        }
    }

    val logBuffer = remember { LogBuffer(maxSize = 2000) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default.limitedParallelism(4)) {
            KxLog.logFlow.collect { log ->
                logBuffer.add(log)
            }
        }
    }

    KlyxTheme(
        dynamicColor = dynamicColor,
        darkTheme = darkTheme,
        useThemeExtension = useThemeExtension,
        themeName = themeName
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val background = remember(colorScheme) { colorScheme.background }

        LaunchedEffect(Unit) {
            lifecycleOwner.subscribeToEvent<KeyEvent> { event ->
                if (event.isCtrlPressed && event.isShiftPressed && event.key == Key.P) {
                    CommandManager.showPalette()
                }
            }
        }

        CompositionLocalProvider(
            LocalDrawerState provides drawerState
        ) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(
                        drawerState = drawerState,
                        modifier = Modifier
                            .width(DrawerWidth)
                            .fillMaxHeight()
                    ) {
                        if (project.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                TextButtonWithShortcut(
                                    text = "Open a project",
                                    modifier = Modifier.padding(top = 20.dp),
                                    shortcut = keyShortcutOf(ctrl = true, alt = true, key = Key.O)
                                ) {
                                    directoryPicker.launch()
                                }
                            }
                        } else {
                            FileTree(
                                rootNodes = project.toFileTreeNodes(),
                                modifier = Modifier.fillMaxSize(),
                                onFileLongClick = {

                                },
                                onFileClick = {
                                    editorViewModel.openFile(it)
                                    scope.launch {
                                        drawerState.close()
                                    }
                                }
                            )
                        }
                    }
                },
                gesturesEnabled = drawerState.isOpen
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.surfaceColorAtElevation(5.dp),
                    contentColor = colorScheme.onSurface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        MainMenuBar()

                        if (CommandManager.showCommandPalette) {
                            CommandPalette(
                                commands = CommandManager.commands,
                                recentlyUsedCommands = CommandManager.recentlyUsedCommands,
                                onDismissRequest = CommandManager::hidePalette
                            )
                        }

                        if (ThemeManager.showThemeSelector) {
                            ThemeSelector(
                                onDismissRequest = ThemeManager::hideThemeSelector
                            )
                        }

                        Surface(
                            color = background,
                            modifier = Modifier
                                .imePadding()
                                .systemBarsPadding()
                        ) {
                            Column {
                                val activeFile by editorViewModel.activeFile.collectAsState()
                                val editorState by editorViewModel.currentEditorState.collectAsState()

                                EditorScreen(
                                    modifier = Modifier.weight(1f),
                                    editorViewModel = editorViewModel,
                                    klyxViewModel = klyxViewModel
                                )

                                StatusBar(
                                    modifier = Modifier.fillMaxWidth(),
                                    state = StatusBarState(
                                        editorState = editorState,
                                        language = activeFile?.language()
                                    ),
                                    onLogClick = { klyxViewModel.showLogViewer() }
                                )
                            }
                        }

                        if (appState.showLogViewer) {
                            LogViewerSheet(
                                buffer = logBuffer,
                                onOpenAsTabClick = {
                                    editorViewModel.openLogViewer(logBuffer)
                                },
                                onDismissRequest = klyxViewModel::dismissLogViewer
                            )
                        }
                    }

                    extraContent()
                }
            }

            NotificationOverlay()

            if (appState.showPermissionDialog) {
                PermissionDialog(
                    onDismissRequest = { klyxViewModel.dismissPermissionDialog() },
                    onRequestPermission = { requestFileAccessPermission() }
                )
            }
        }
    }
}

expect fun requestFileAccessPermission()
