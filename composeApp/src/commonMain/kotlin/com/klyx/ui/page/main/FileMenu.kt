package com.klyx.ui.page.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DriveFolderUpload
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.SaveAs
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.core.LocalNotifier
import com.klyx.core.cmd.key.keyShortcutOf
import com.klyx.core.file.KxFile
import com.klyx.core.file.Worktree
import com.klyx.core.file.isKlyxTempFile
import com.klyx.core.file.isPermissionRequired
import com.klyx.core.file.toKxFile
import com.klyx.core.file.toKxFiles
import com.klyx.core.io.R_OK
import com.klyx.core.io.W_OK
import com.klyx.core.ui.component.DropdownMenuDivider
import com.klyx.core.ui.component.DropdownMenuItem
import com.klyx.di.LocalEditorViewModel
import com.klyx.di.LocalFileTreeViewModel
import com.klyx.di.LocalKlyxViewModel
import com.klyx.filetree.asFileTreeNode
import com.klyx.res.Res.string
import com.klyx.res.notification_all_files_saved
import com.klyx.res.notification_failed_to_save
import com.klyx.res.notification_no_active_file
import com.klyx.res.notification_no_files_to_save
import com.klyx.res.notification_saved
import com.klyx.tab.FileTab
import com.klyx.ui.util.openIfClosed
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.itsvks.anyhow.onFailure
import io.itsvks.anyhow.onSuccess
import kotlinx.coroutines.launch

@Composable
fun FileMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
) {
    val editorViewModel = LocalEditorViewModel.current
    val klyxViewModel = LocalKlyxViewModel.current
    val fileTreeViewModel = LocalFileTreeViewModel.current

    val notifier = LocalNotifier.current
    val drawerState = LocalDrawerState.current
    val scope = rememberCoroutineScope()

    val openDrawerIfClosed = fun() {
        scope.launch { drawerState.openIfClosed() }
    }

    val filePicker = rememberFilePickerLauncher(mode = FileKitMode.Multiple()) { files ->
        files?.toKxFiles()?.forEach(editorViewModel::openFile)
        onDismissRequest()
    }

    val fileSaver = rememberFileSaverLauncher { file ->
        if (file != null) {
            editorViewModel.saveCurrentAs(file.toKxFile())
                .onSuccess { notifier.toast(com.klyx.core.util.string(string.notification_saved)) }
                .onFailure { notifier.error(com.klyx.core.util.string(string.notification_failed_to_save, file.name)) }
        }
        onDismissRequest()
    }

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
        onDismissRequest()
    }

    val addFolderPicker = rememberDirectoryPickerLauncher { file ->
        if (file != null) {
            val kx = file.toKxFile()

            if (kx.isPermissionRequired(R_OK or W_OK)) {
                klyxViewModel.showPermissionDialog()
            } else {
                val worktree = Worktree(kx)
                klyxViewModel.addWorktreeToProject(worktree)
                scope.launch { fileTreeViewModel.selectNode(worktree.asFileTreeNode()) }
                openDrawerIfClosed()
            }
        }
        onDismissRequest()
    }

    val activeFile by editorViewModel.activeFile.collectAsStateWithLifecycle()
    val activeTab by editorViewModel.activeTab.collectAsStateWithLifecycle()

    DropdownMenu(
        expanded = expanded,
        offset = DpOffset((-5).dp, 0.dp),
        shape = MaterialTheme.shapes.medium,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = "New",
            onClick = {
                editorViewModel.openFile(KxFile("untitled"))
                onDismissRequest()
            },
            icon = {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Create new file"
                )
            },
            shortcut = keyShortcutOf(ctrl = true, key = Key.N)
        )

        DropdownMenuDivider()

        DropdownMenuItem(
            text = { Text("Open File...") },
            onClick = { filePicker.launch() },
            leadingIcon = {
                Icon(
                    Icons.Outlined.FileOpen,
                    contentDescription = "Open File"
                )
            }
        )

        DropdownMenuItem(
            text = "Open Folder...",
            onClick = { directoryPicker.launch() },
            icon = {
                Icon(
                    Icons.Outlined.DriveFolderUpload,
                    contentDescription = "Open Folder"
                )
            },
            shortcut = keyShortcutOf(ctrl = true, key = Key.O)
        )

        DropdownMenuDivider()

        DropdownMenuItem(
            text = { Text("Add Folder to Project...") },
            onClick = { addFolderPicker.launch() },
            leadingIcon = {
                Icon(
                    Icons.Outlined.CreateNewFolder,
                    contentDescription = "Add Folder to Project"
                )
            }
        )

        DropdownMenuDivider()

        activeTab?.let { tab ->
            if (tab is FileTab && !tab.isInternal) {
                DropdownMenuItem(
                    text = "Save",
                    onClick = {
                        val file = activeFile
                        if (file == null) {
                            notifier.notify(com.klyx.core.util.string(string.notification_no_active_file))
                            return@DropdownMenuItem
                        }

                        if (file.isKlyxTempFile()) {
                            fileSaver.launch(file.name)
                        } else {
                            editorViewModel.saveCurrent()
                                .onSuccess { notifier.toast(com.klyx.core.util.string(string.notification_saved)) }
                                .onFailure { notifier.error(com.klyx.core.util.string(string.notification_failed_to_save, file.name)) }
                        }
                        onDismissRequest()
                    },
                    enabled = tab.isModified,
                    icon = {
                        Icon(
                            Icons.Outlined.Save,
                            contentDescription = "Save"
                        )
                    },
                    shortcut = keyShortcutOf(ctrl = true, key = Key.S)
                )

                DropdownMenuItem(
                    text = "Save As...",
                    onClick = {
                        val file = activeFile
                        if (file == null) {
                            notifier.notify(com.klyx.core.util.string(string.notification_no_active_file))
                            onDismissRequest()
                            return@DropdownMenuItem
                        }
                        fileSaver.launch(file.name)
                    },
                    icon = {
                        Icon(
                            Icons.Outlined.SaveAs,
                            contentDescription = "Save As"
                        )
                    },
                    shortcut = keyShortcutOf(ctrl = true, shift = true, key = Key.S)
                )

                DropdownMenuItem(
                    text = "Save All",
                    onClick = {
                        val results = editorViewModel.saveAll()
                        if (results.isEmpty()) {
                            notifier.notify(com.klyx.core.util.string(string.notification_no_files_to_save))
                        } else {
                            val failedFiles = results.filter { !it.value }.keys
                            if (failedFiles.isEmpty()) {
                                notifier.toast(com.klyx.core.util.string(string.notification_all_files_saved))
                            } else {
                                notifier.error(
                                    com.klyx.core.util.string(
                                        string.notification_failed_to_save,
                                        failedFiles.joinToString(", ")
                                    )
                                )
                            }
                        }
                        onDismissRequest()
                    },
                    icon = {
                        Icon(
                            Icons.Outlined.Save,
                            contentDescription = "Save All"
                        )
                    },
                    shortcut = keyShortcutOf(ctrl = true, alt = true, key = Key.S)
                )

                DropdownMenuDivider()
            }
        }
    }
}
