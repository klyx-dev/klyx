package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import com.klyx.LocalDrawerState
import com.klyx.openIfClosed
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.CommandManager.addCommands
import com.klyx.core.cmd.CommandManager.removeCommands
import com.klyx.core.cmd.buildCommand
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.file.KxFile
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.file.toKxFile
import com.klyx.core.file.toKxFiles
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.di.LocalEditorViewModel
import com.klyx.di.LocalKlyxViewModel
import com.klyx.extension.api.Worktree
import com.klyx.res.Res
import com.klyx.res.notification_all_files_saved
import com.klyx.res.notification_failed_to_save
import com.klyx.res.notification_no_active_file
import com.klyx.res.notification_no_files_to_save
import com.klyx.res.notification_saved
import com.klyx.ui.page.main.closeCurrentWindow
import com.klyx.ui.page.main.openNewWindow
import com.klyx.ui.page.main.quitApp
import com.klyx.viewmodel.openExtensionScreen
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.launch

@Suppress("ComposableNaming")
@Composable
internal fun registerGeneralCommands() {
    val editorViewModel = LocalEditorViewModel.current
    val klyxViewModel = LocalKlyxViewModel.current

    val context = LocalPlatformContext.current
    val notifier = LocalNotifier.current
    val drawerState = LocalDrawerState.current
    val coroutineScope = rememberCoroutineScope()

    val filePicker = rememberFilePickerLauncher(mode = FileKitMode.Multiple()) { files ->
        files?.toKxFiles()?.forEach(editorViewModel::openFile)
    }

    val fileSaver = rememberFileSaverLauncher { savedFile ->
        if (savedFile != null) {
            val saved = editorViewModel.saveCurrentAs(savedFile.toKxFile())
            if (saved) notifier.toast(string(Res.string.notification_saved))
        }
    }

    val projectPicker = rememberDirectoryPickerLauncher {
        if (it != null) {
            val dir = it.toKxFile()
            if (dir.isPermissionRequired(R_OK or W_OK)) klyxViewModel.showPermissionDialog()
            else {
                klyxViewModel.openProject(Worktree(dir))
                coroutineScope.launch { drawerState.openIfClosed() }
            }
        }
    }

    val commands = remember {
        listOf(
            buildCommand {
                hideInCommandPalette()
                shortcut(keyShortcutOf(ctrl = true, key = Key.O))
                action { projectPicker.launch() }
            },

            buildCommand {
                name("workspace: new window")
                shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.N))
                action { openNewWindow(context) }
            },

            buildCommand {
                name("workspace: close active item")
                shortcut(keyShortcutOf(ctrl = true, key = Key.W))
                action { editorViewModel.closeActiveTab() }
            },

//            buildCommand {
//                name("command palette: toggle")
//                dismissOnAction(false)
//                shortcut(keyShortcutOf(shift = true, key = Key.ShiftLeft))
//                action { CommandManager.toggleCommandPalette() }
//            },

            buildCommand {
                name("klyx: command palette")
                hideInCommandPalette()
                shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.P))
                action { CommandManager.showCommandPalette() }
            },

            buildCommand {
                name("klyx: extensions")
                shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.X))
                action { editorViewModel.openExtensionScreen() }
            },

            buildCommand {
                name("workspace: close window")
                shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.W))
                action { closeCurrentWindow(context) }
            },

            buildCommand {
                name("klyx: quit")
                shortcut(keyShortcutOf(key = Key.Q, ctrl = true))
                action { quitApp() }
            },

            buildCommand {
                name("workspace: new file")
                shortcut(keyShortcutOf(ctrl = true, key = Key.N))
                action { editorViewModel.openFile(KxFile("untitled")) }
            },

            buildCommand {
                name("workspace: open files")
                action { filePicker.launch() }
            },

            buildCommand {
                name("workspace: save")
                shortcut(keyShortcutOf(ctrl = true, key = Key.S))
                action {
                    val file = editorViewModel.activeFile.value
                    if (file == null) {
                        notifier.notify(string(Res.string.notification_no_active_file))
                        return@action
                    }

                    if (file.path == "/untitled") {
                        fileSaver.launch(file.name)
                    } else {
                        val saved = editorViewModel.saveCurrent()
                        if (saved) notifier.toast(string(Res.string.notification_saved))
                    }
                }
            },

            buildCommand {
                name("workspace: save as")
                shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.S))
                action {
                    val file = editorViewModel.activeFile.value
                    if (file == null) {
                        notifier.notify(string(Res.string.notification_no_active_file))
                        return@action
                    }
                    fileSaver.launch(file.name)
                }
            },

            buildCommand {
                name("workspace: save all")
                shortcut(keyShortcutOf(ctrl = true, alt = true, key = Key.S))
                action {
                    val results = editorViewModel.saveAll()
                    if (results.isEmpty()) {
                        notifier.notify(string(Res.string.notification_no_files_to_save))
                    } else {
                        val failedFiles = results.filter { !it.value }.keys
                        if (failedFiles.isEmpty()) {
                            notifier.toast(string(Res.string.notification_all_files_saved))
                        } else {
                            notifier.error(
                                string(
                                    Res.string.notification_failed_to_save,
                                    failedFiles.joinToString(", ")
                                )
                            )
                        }
                    }
                }
            }
        )
    }

    DisposableEffect(editorViewModel) {
        addCommands(commands)
        onDispose { removeCommands(commands) }
    }
}
