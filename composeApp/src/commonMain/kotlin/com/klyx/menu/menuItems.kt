package com.klyx.menu

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import com.klyx.LocalDrawerState
import com.klyx.core.DOCS_URL
import com.klyx.core.Environment
import com.klyx.core.KEYBOARD_SHORTCUTS_URL
import com.klyx.core.LocalNotifier
import com.klyx.core.Notifier
import com.klyx.core.REPORT_ISSUE_URL
import com.klyx.core.cmd.CommandManager
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.core.logging.logger
import com.klyx.platform.PlatformInfo
import com.klyx.res.Res.string
import com.klyx.res.file_menu_title
import com.klyx.res.help_menu_title
import com.klyx.res.klyx_menu_title
import com.klyx.res.menu_item_about_klyx
import com.klyx.res.menu_item_add_folder_to_project
import com.klyx.res.menu_item_close_project
import com.klyx.res.menu_item_command_palette
import com.klyx.res.menu_item_documentation
import com.klyx.res.menu_item_extensions
import com.klyx.res.menu_item_keyboard_shortcuts
import com.klyx.res.menu_item_new_file
import com.klyx.res.menu_item_open_default_settings
import com.klyx.res.menu_item_open_file
import com.klyx.res.menu_item_open_folder
import com.klyx.res.menu_item_open_settings
import com.klyx.res.menu_item_quit
import com.klyx.res.menu_item_report_issue
import com.klyx.res.menu_item_restart_app
import com.klyx.res.menu_item_save
import com.klyx.res.menu_item_save_all
import com.klyx.res.menu_item_save_as
import com.klyx.res.notification_all_files_saved
import com.klyx.res.notification_failed_to_save
import com.klyx.res.notification_no_active_file
import com.klyx.res.notification_no_files_to_save
import com.klyx.res.notification_saved
import com.klyx.viewmodel.EditorViewModel
import com.klyx.viewmodel.KlyxViewModel
import com.klyx.viewmodel.openDefaultSettings
import com.klyx.viewmodel.openExtensionScreen
import com.klyx.viewmodel.openSettings
import com.klyx.viewmodel.showWelcome
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

internal expect fun openSystemTerminal()
internal expect fun restartApp(isKillProcess: Boolean = true)
internal expect fun quitApp(): Nothing

private val fs = SystemFileSystem
private val logger = logger("Klyx")

@Composable
fun rememberMenuItems(
    viewModel: EditorViewModel,
    klyxViewModel: KlyxViewModel
): Map<String, List<MenuItem>> {
    val notifier = LocalNotifier.current
    val uriHandler = LocalUriHandler.current
    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    val openDrawerIfClosed = {
        if (drawerState.isClosed) {
            scope.launch { drawerState.open() }
        }
    }

    val filePicker = rememberFilePickerLauncher { file ->
        if (file != null) {
            viewModel.openFile(file.toKxFile())
        }
    }

    val fileSaver = rememberFileSaverLauncher { file ->
        if (file != null) {
            val saved = viewModel.saveAs(file.toKxFile())
            if (saved) notifier.success(com.klyx.core.string(string.notification_saved))
        }
    }

    val directoryPicker = rememberDirectoryPickerLauncher { file ->
        if (file != null) {
            klyxViewModel.openProject(file.toKxFile())
            openDrawerIfClosed()
        }
    }

    val addFolderPicker = rememberDirectoryPickerLauncher {
        if (it != null) {
            klyxViewModel.addFolderToProject(it.toKxFile())
            openDrawerIfClosed()
        }
    }

    return remember(viewModel, klyxViewModel) {
        menu {
            klyxMenuGroup(viewModel, klyxViewModel, notifier)

            fileMenuGroup(
                onNewFileClick = { viewModel.openFile(KxFile("untitled")) },
                onOpenFileClick = { filePicker.launch() },
                onOpenFolderClick = { directoryPicker.launch() },
                onAddFolderClick = { addFolderPicker.launch() },
                onSaveClick = {
                    val file = viewModel.getActiveFile()
                    if (file == null) {
                        notifier.notify(com.klyx.core.string(string.notification_no_active_file))
                        return@fileMenuGroup
                    }

                    if (file.path == "/untitled") {
                        fileSaver.launch(file.name, extension = file.extension.ifEmpty { "txt" })
                    } else {
                        val saved = viewModel.saveCurrent()
                        if (saved) notifier.toast(com.klyx.core.string(string.notification_saved))
                    }
                },
                onSaveAsClick = {
                    val file = viewModel.getActiveFile()
                    if (file == null) {
                        notifier.notify(com.klyx.core.string(string.notification_no_active_file))
                        return@fileMenuGroup
                    }
                    fileSaver.launch(file.name)
                },
                onSaveAllClick = {
                    val results = viewModel.saveAll()
                    if (results.isEmpty()) {
                        notifier.notify(com.klyx.core.string(string.notification_no_files_to_save))
                    } else {
                        val failedFiles = results.filter { !it.value }.keys
                        if (failedFiles.isEmpty()) {
                            notifier.toast(com.klyx.core.string(string.notification_all_files_saved))
                        } else {
                            notifier.error(
                                com.klyx.core.string(
                                    string.notification_failed_to_save,
                                    failedFiles.joinToString(", ")
                                )
                            )
                        }
                    }
                },
                onCloseProjectClick = { klyxViewModel.closeProject() }
            )

            helpMenuGroup(viewModel, uriHandler)
        }
    }
}

private fun MenuBuilder.klyxMenuGroup(
    editorViewModel: EditorViewModel,
    klyxViewModel: KlyxViewModel,
    notifier: Notifier
) = group(string.klyx_menu_title) {
    item(string.menu_item_about_klyx) {
        onClick { klyxViewModel.showAboutDialog() }
    }

    divider()

    item(string.menu_item_open_settings) {
        shortcut(keyShortcutOf(Key.Comma, ctrl = true))
        onClick { editorViewModel.openSettings() }
    }

    item(string.menu_item_open_default_settings) {
        onClick { editorViewModel.openDefaultSettings() }
    }

    divider()

    item("Terminal") {
        onClick { openSystemTerminal() }
    }

    item(string.menu_item_command_palette) {
        shortcut(keyShortcutOf(Key.P, ctrl = true, shift = true))
        onClick { CommandManager.showPalette() }
    }

    item(string.menu_item_extensions) {
        shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.X))
        onClick { editorViewModel.openExtensionScreen() }
    }

    if (PlatformInfo.isDebugBuild) {
        divider()

        "Clear Old Logs" {
            try {
                fs.delete(Path(Environment.LogsDir))
                notifier.success("All logs cleared.")
            } catch (e: Exception) {
                logger.error(e.message.orEmpty(), e)
                notifier.error("Failed to clear logs.")
            }
        }
    }

    divider()

    if (PlatformInfo.isAndroid) {
        item(string.menu_item_restart_app) {
            onClick { restartApp(true) }
        }
    }

    item(string.menu_item_quit) {
        shortcut(keyShortcutOf(ctrl = true, key = Key.Q))
        onClick { quitApp() }
    }
}

private fun MenuBuilder.fileMenuGroup(
    onNewFileClick: suspend () -> Unit = {},
    onOpenFileClick: suspend () -> Unit = {},
    onOpenFolderClick: suspend () -> Unit = {},
    onAddFolderClick: suspend () -> Unit = {},
    onSaveClick: suspend () -> Unit = {},
    onSaveAsClick: suspend () -> Unit = {},
    onSaveAllClick: suspend () -> Unit = {},
    onCloseProjectClick: suspend () -> Unit = {}
) = group(string.file_menu_title) {
    item(string.menu_item_new_file) {
        shortcut(keyShortcutOf(ctrl = true, key = Key.N))
        onClick { withContext(Dispatchers.Default) { onNewFileClick() } }
    }

    divider()

    item(string.menu_item_open_file) {
        onClick { withContext(Dispatchers.Default) { onOpenFileClick() } }
    }

    item(string.menu_item_open_folder) {
        shortcut(keyShortcutOf(ctrl = true, key = Key.O))
        onClick { withContext(Dispatchers.Default) { onOpenFolderClick() } }
    }

    divider()

    item(string.menu_item_add_folder_to_project) {
        onClick { withContext(Dispatchers.Default) { onAddFolderClick() } }
    }

    divider()

    item(string.menu_item_save) {
        shortcut(keyShortcutOf(ctrl = true, key = Key.S))
        onClick { withContext(Dispatchers.Default) { onSaveClick() } }
    }

    item(string.menu_item_save_as) {
        shortcut(keyShortcutOf(ctrl = true, shift = true, key = Key.S))
        onClick { withContext(Dispatchers.Default) { onSaveAsClick() } }
    }

    item(string.menu_item_save_all) {
        shortcut(keyShortcutOf(ctrl = true, alt = true, key = Key.S))
        onClick { withContext(Dispatchers.Default) { onSaveAllClick() } }
    }

    divider()

    item(string.menu_item_close_project) {
        onClick { withContext(Dispatchers.Default) { onCloseProjectClick() } }
    }
}

private fun MenuBuilder.helpMenuGroup(
    viewModel: EditorViewModel,
    uriHandler: UriHandler
) = group(string.help_menu_title) {
    item(string.menu_item_documentation) {
        onClick { uriHandler.openUri(DOCS_URL) }
    }

    item(string.menu_item_keyboard_shortcuts) {
        onClick { uriHandler.openUri(KEYBOARD_SHORTCUTS_URL) }
    }

    "Show Welcome"(viewModel::showWelcome)

    divider()

    item(string.menu_item_report_issue) {
        onClick { uriHandler.openUri(REPORT_ISSUE_URL) }
    }
}
