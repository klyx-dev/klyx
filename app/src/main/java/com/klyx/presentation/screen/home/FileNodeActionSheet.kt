package com.klyx.presentation.screen.home

import android.content.ClipData
import android.net.Uri
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.toClipEntry
import com.klyx.api.data.file.KxFile
import com.klyx.data.file.openWith
import com.klyx.data.file.share
import com.klyx.data.file.shareableUri
import com.klyx.presentation.components.CloseProject
import com.klyx.presentation.components.Copy
import com.klyx.presentation.components.CopyPath
import com.klyx.presentation.components.Cut
import com.klyx.presentation.components.Delete
import com.klyx.presentation.components.FileActionBottomSheet
import com.klyx.presentation.components.NewDirectory
import com.klyx.presentation.components.NewFile
import com.klyx.presentation.components.OpenWith
import com.klyx.presentation.components.Paste
import com.klyx.presentation.components.Rename
import com.klyx.presentation.components.Share
import com.klyx.presentation.components.filetree.FileNode
import com.klyx.presentation.viewmodel.EditorViewModel
import com.klyx.presentation.viewmodel.FileTreeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileNodeActionSheet(
    node: FileNode,
    isProject: Boolean,
    fileTreeViewModel: FileTreeViewModel,
    editorViewModel: EditorViewModel,
    scope: CoroutineScope,
    clipboard: Clipboard,
    onDismissRequest: () -> Unit,
    onDeleteNode: (FileNode) -> Unit,
    onRenameNode: (FileNode) -> Unit,
    onCreateFile: (FileNode) -> Unit,
    onCreateFolder: (FileNode) -> Unit,
) {
    val sheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded)
    )

    val dismiss: () -> Unit = {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            if (!sheetState.isVisible) {
                onDismissRequest()
            }
        }
    }

    FileActionBottomSheet(
        file = node.file,
        isProject = isProject,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        onFileAction = { action ->
            when (action) {
                is Copy -> {
                    fileTreeViewModel.copyNode(node)
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipData.newRawUri("file", action.file.shareableUri)
                                .toClipEntry()
                        )
                    }
                    dismiss()
                }

                is Cut -> {
                    fileTreeViewModel.cutNode(node)
                    scope.launch {
                        clipboard.setClipEntry(
                            ClipData.newRawUri("file", action.file.shareableUri)
                                .toClipEntry()
                        )
                    }
                    dismiss()
                }

                is CopyPath -> {
                    scope.launch {
                        copyPath(action.file, clipboard)
                        dismiss()
                    }
                }

                is Delete -> {
                    onDeleteNode(node)
                    dismiss()
                }

                is Rename -> {
                    onRenameNode(node)
                    dismiss()
                }

                is OpenWith -> action.file.openWith()
                is Share -> action.file.share()

                is Paste -> {
                    pasteFromClipboard(
                        destinationUri = action.destination.uri,
                        fileTreeViewModel = fileTreeViewModel,
                        editorViewModel = editorViewModel,
                        clipboard = clipboard,
                        scope = scope
                    )
                    dismiss()
                }
            }
        },
        onDirectoryAction = { action ->
            when (action) {
                is CloseProject -> {
                    fileTreeViewModel.removeRootNode(action.file)
                    dismiss()
                }

                is CopyPath -> {
                    scope.launch {
                        copyPath(action.file, clipboard)
                        dismiss()
                    }
                }

                is Delete -> {
                    onDeleteNode(node)
                    dismiss()
                }

                is Rename -> {
                    onRenameNode(node)
                    dismiss()
                }

                is NewFile -> {
                    onCreateFile(node)
                    dismiss()
                }

                is NewDirectory -> {
                    onCreateFolder(node)
                    dismiss()
                }

                is Paste -> {
                    pasteFromClipboard(
                        destinationUri = action.destination.uri,
                        fileTreeViewModel = fileTreeViewModel,
                        editorViewModel = editorViewModel,
                        clipboard = clipboard,
                        scope = scope
                    )
                    dismiss()
                }
            }
        }
    )
}

private fun pasteFromClipboard(
    destinationUri: Uri,
    fileTreeViewModel: FileTreeViewModel,
    editorViewModel: EditorViewModel,
    clipboard: Clipboard,
    scope: CoroutineScope,
) {
    fileTreeViewModel
        .visibleNodes
        .value
        .firstNotNullOfOrNull {
            if (it.node.uri == destinationUri) {
                it.node
            } else {
                null
            }
        }?.let { parentNode ->
            scope.launch {
                val clipUri =
                    clipboard.getClipEntry()?.clipData?.getItemAt(0)?.uri
                fileTreeViewModel.pasteNode(
                    targetParent = parentNode,
                    clipboardUri = clipUri,
                    onMoveCompleted = editorViewModel::handleFileRenamed
                )
            }
        }
}

private suspend fun copyPath(file: KxFile, clipboard: Clipboard) {
    clipboard.setClipEntry(ClipData.newPlainText("klyx", (file.uri.path ?: file.uri.toString())).toClipEntry())
}
