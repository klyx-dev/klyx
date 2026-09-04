package com.klyx.presentation.components.filetree

import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetValue.PartiallyExpanded
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.api.data.fs.Paths
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.terminal.home
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.Screen
import com.klyx.presentation.viewmodel.FileTreeUiState
import com.klyx.presentation.viewmodel.FileTreeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTreeDrawer(
    viewModel: FileTreeViewModel,
    modifier: Modifier = Modifier,
    drawerState: DrawerState = rememberDrawerState(initialValue = DrawerValue.Closed),
    gesturesEnabled: Boolean = false,
    onFileClick: (node: FileNode, rootNode: FileNode) -> Unit = { _, _ -> },
    onFileLongClick: (node: FileNode, rootNode: FileNode) -> Unit = { _, _ -> },
    screenContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val navigator = LocalNavigator.current
    val drawerWidth = FileTree.drawerWidth()

    val fraction by remember {
        derivedStateOf {
            val widthPx = with(density) { drawerWidth.toPx() }
            val offset = drawerState.currentOffset
            (1f + (offset / widthPx)).coerceIn(0f, 1f)
        }
    }

    var showLocationPicker by remember { mutableStateOf(false) }
    val sheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded)
    )

    var showSearchSheet by remember { mutableStateOf(false) }
    var showSftpSheet by remember { mutableStateOf(false) }
    val sftpSheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded)
    )
    val searchSheetState = rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, PartiallyExpanded, Expanded)
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showTrashEntry = LocalAppSettings.current.fileTree.useTrash

    val directoryPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }.onFailure { it.printStackTrace() }
                val file = DocumentFile.fromTreeUri(context, uri)!!
                viewModel.addRootNode(file.uri)
            }
        }

    ModalNavigationDrawer(
        modifier = modifier,
        gesturesEnabled = gesturesEnabled,
        drawerState = drawerState,
        drawerContent = {
            FileTreeDrawerSheet(
                drawerState = drawerState,
                drawerWidth = drawerWidth,
                uiState = uiState,
                onAddFolderClick = { showLocationPicker = true },
                onSearchClick = { showSearchSheet = true },
                onTrashClick = if (showTrashEntry) {
                    {
                        navigator.navigateTo(Trash)
                        scope.launch { drawerState.close() }
                    }
                } else null,
                viewModel = viewModel,
                onNodeClick = onFileClick,
                onNodeLongClick = onFileLongClick
            )
        },
        content = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset {
                        val x = (drawerWidth * fraction).toPx()
                        val extraOffset = -12.dp.toPx() * fraction

                        IntOffset(
                            x = (x + extraOffset).fastRoundToInt(),
                            y = 0
                        )
                    }
            ) {
                screenContent()
            }
        }
    )

    if (showLocationPicker) {
        fun dismiss() {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    showLocationPicker = false
                }
            }
        }

        ProjectLocationBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { showLocationPicker = false },
            onSelectSystemPicker = {
                dismiss()
                directoryPicker.launch(null)
            },
            onSelectInternalStorage = {
                dismiss()
                viewModel.addRootNode(Uri.fromFile(Environment.getExternalStorageDirectory()))
            },
            onSelectAppDirectory = {
                dismiss()
                viewModel.addRootNode(Uri.fromFile(context.dataDir))
            },
            onSelectTerminalHome = {
                dismiss()
                viewModel.addRootNode(Uri.fromFile(Paths.home))
            },
            onSelectSftp = {
                showSftpSheet = true
            }
        )
    }

    if (showSftpSheet) {
        fun cancel() {
            scope.launch { sftpSheetState.hide() }.invokeOnCompletion {
                if (!sftpSheetState.isVisible) {
                    showSftpSheet = false
                }
            }
        }

        SftpConnectionSheet(
            sheetState = sftpSheetState,
            onDismissRequest = { showSftpSheet = false },
            onCancel = { cancel() },
            onConnect = { host, port, username, password, path ->
                scope.launch { sftpSheetState.hide() }.invokeOnCompletion {
                    if (!sftpSheetState.isVisible) {
                        showSftpSheet = false
                        showLocationPicker = false
                    }
                }
                val userPart = if (password != null) "$username:$password" else username
                val uri = Uri.parse("sftp://$userPart@$host:$port$path")
                viewModel.addRootNode(uri)
            }
        )
    }

    if (showSearchSheet) {
        fun dismiss() {
            scope.launch { searchSheetState.hide() }.invokeOnCompletion {
                if (!searchSheetState.isVisible) {
                    showSearchSheet = false
                }
            }
        }

        SearchBottomSheet(
            sheetState = searchSheetState,
            onDismissRequest = { showSearchSheet = false },
            onQueryChange = viewModel::search,
            query = uiState.searchQuery,
            searchResultCount = uiState.searchResultCount,
            isSearching = uiState.isSearching,
            showFdHint = !uiState.hasFastSearch && uiState.rootNodes.any { it.uri.scheme == "file" },
            searchEventFlow = viewModel.searchEventFlow,
            searchRoots = uiState.rootNodes,
            onResultClick = { file ->
                viewModel.selectSearchResult(file)
                scope.launch { drawerState.open() }
                dismiss()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileTreeDrawerSheet(
    drawerState: DrawerState,
    drawerWidth: Dp,
    uiState: FileTreeUiState,
    onAddFolderClick: () -> Unit,
    onSearchClick: () -> Unit,
    onTrashClick: (() -> Unit)?,
    viewModel: FileTreeViewModel,
    onNodeClick: (node: FileNode, rootNode: FileNode) -> Unit,
    onNodeLongClick: (node: FileNode, rootNode: FileNode) -> Unit,
) {
    ModalDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier.width(drawerWidth),
        drawerContentColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        val isOpening = drawerState.targetValue == DrawerValue.Open
        val isFullyClosed =
            drawerState.currentValue == DrawerValue.Closed && drawerState.targetValue == DrawerValue.Closed

        if (uiState.rootNodes.isEmpty()) {
            EmptyState(
                isOpening = isOpening,
                isFullyClosed = isFullyClosed,
                onOpenProjectClick = onAddFolderClick
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                DrawerActionBar(
                    onAddFolderClick = onAddFolderClick,
                    onSearchClick = onSearchClick,
                    onTrashClick = onTrashClick
                )

                FileTree(
                    viewModel = viewModel,
                    onNodeClick = onNodeClick,
                    onNodeLongClick = onNodeLongClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
