package com.klyx.presentation.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.klyx.api.data.editor.SaveAs
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.data.file.wrap
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.data.runner.FileRunRequest
import com.klyx.api.data.runner.FileRunnerRegistry
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.ui.ScreenRegistry
import com.klyx.api.ui.ToolbarRegistry
import com.klyx.api.ui.showFailureToast
import com.klyx.api.ui.theme.JetBrainsMonoFontFamily
import com.klyx.core.globalOf
import com.klyx.data.editor.EditorStateRegistry
import com.klyx.data.runner.FileRunnerContextImpl
import com.klyx.data.runner.TerminalCommandRunner
import com.klyx.i18n.getLocaleStrings
import com.klyx.presentation.components.UnsupportedFileDialog
import com.klyx.presentation.components.dialogs.CloseUnsavedTabDialog
import com.klyx.presentation.components.dialogs.DeleteFileDialog
import com.klyx.presentation.components.dialogs.NewFileDialog
import com.klyx.presentation.components.dialogs.NewFolderDialog
import com.klyx.presentation.components.dialogs.RenameFileDialog
import com.klyx.presentation.components.filetree.FileNode
import com.klyx.presentation.components.filetree.FileTreeDrawer
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.screen.home.FileNodeActionSheet
import com.klyx.presentation.screen.home.HomeShareDialogs
import com.klyx.presentation.screen.home.HomeTopBar
import com.klyx.presentation.screen.home.MainContent
import com.klyx.presentation.screen.home.MenuAction
import com.klyx.presentation.viewmodel.EditorEvent
import com.klyx.presentation.viewmodel.EditorViewModel
import com.klyx.presentation.viewmodel.FileTreeViewModel
import com.klyx.presentation.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = koinViewModel(),
    editorViewModel: EditorViewModel = koinViewModel(),
    fileTreeViewModel: FileTreeViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val registry: EditorStateRegistry = koinInject()

    val toastHostState = LocalToastHostState.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedNodeForAction by remember { mutableStateOf<FileNode?>(null) }
    val jbFontFamily = remember { JetBrainsMonoFontFamily }

    val editorUiState by editorViewModel.uiState.collectAsStateWithLifecycle()
    val openTabs by editorViewModel.openTabs.collectAsStateWithLifecycle()
    val activeTab by editorViewModel.activeTab.collectAsStateWithLifecycle()

    val navigator = LocalNavigator.current
    val fileRunnerRegistry = globalOf<FileRunnerRegistry>()
    val screenRegistry = globalOf<ScreenRegistry>()
    val terminalRunner: TerminalCommandRunner = koinInject()

    val runnableTab = activeTab as? WorkspaceTab.TextFile
    val canRun = runnableTab != null &&
            fileRunnerRegistry.supports(
                FileRunRequest(runnableTab.file, runnableTab.file.uri, runnableTab.projectUri, runnableTab.id)
            )

    val onRunFile: (() -> Unit)? = if (runnableTab != null && canRun) {
        {
            val tab = runnableTab
            val request = FileRunRequest(tab.file, tab.file.uri, tab.projectUri, tab.id)
            val runner = fileRunnerRegistry.runnerFor(request)
            if (runner != null) {
                scope.launch {
                    runCatching {
                        editorViewModel.saveFileSuspending(tab.file)
                        runner.run(
                            request,
                            FileRunnerContextImpl(
                                terminalRunner = terminalRunner,
                                navigator = navigator,
                                screenRegistry = screenRegistry,
                                openTab = editorViewModel::openTab,
                            )
                        )
                    }.onFailure { t ->
                        val s = getLocaleStrings()
                        toastHostState.showFailureToast(s.couldNotOpenFile(t.localizedMessage))
                    }
                }
            }
        }
    } else {
        null
    }

    LaunchedEffect(editorViewModel.events, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            editorViewModel.events.collect { event ->
                when (event) {
                    is EditorEvent.ShowError -> toastHostState.showFailureToast(event.error)
                    is EditorEvent.ShowMessage -> toastHostState.showToast(event.message)
                }
            }
        }
    }

    LaunchedEffect(fileTreeViewModel.errorEvent, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            fileTreeViewModel.errorEvent.collect { error ->
                toastHostState.showFailureToast(error)
            }
        }
    }

    editorUiState.unsupportedFileAlert?.let { alert ->
        UnsupportedFileDialog(
            fileName = alert.file.name,
            onDismiss = {
                if (alert.projectUri != null) {
                    scope.launch { drawerState.open() }
                }
                editorViewModel.dismissUnsupportedFileDialog()
            }
        )
    }

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
                fileTreeViewModel.addRootNode(file.uri)
                scope.launch { drawerState.open() }
            }
        }

    val newFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            if (uri != null) {
                editorViewModel.openFile(uri)
            }
        }

    val saveAsLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
            if (uri != null) {
                val newFile = uri.wrap()
                if (activeTab != null) {
                    editorViewModel.handleEditorActions(
                        SaveAs(
                            oldTabId = activeTab!!.id,
                            newFile = newFile
                        )
                    )
                } else {
                    scope.launch {
                        val s = getLocaleStrings()
                        toastHostState.showFailureToast(s.installationFailed(s.noActiveTab))
                    }
                }
            }
        }

    var showShareDialog by remember { mutableStateOf(false) }

    FileTreeDrawer(
        viewModel = fileTreeViewModel,
        gesturesEnabled = openTabs.isEmpty() || drawerState.isOpen,
        onFileClick = { node, rootNode ->
            scope.launch { drawerState.close() }
            editorViewModel.openFile(node.uri, rootNode.uri)
        },
        onFileLongClick = { node, _ ->
            selectedNodeForAction = node
        },
        drawerState = drawerState,
        screenContent = {
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = Transparent,
                topBar = {
                    val toolbarActions = globalOf<ToolbarRegistry>().actions()

                    HomeTopBar(
                        scrollBehavior = scrollBehavior,
                        drawerState = drawerState,
                        scope = scope,
                        openTabs = openTabs,
                        activeTab = activeTab,
                        toolbarActions = toolbarActions,
                        runnable = canRun,
                        onRun = onRunFile,
                        onTabClick = editorViewModel::selectTab,
                        onTabClose = editorViewModel::closeTab,
                        onTabCloseOthers = editorViewModel::closeOtherTabs,
                        onTabCloseAll = editorViewModel::closeAllTabs,
                        onAction = editorViewModel::handleEditorActions,
                        onSaveAsClick = {
                            if (activeTab is WorkspaceTab.TextFile) {
                                saveAsLauncher.launch(activeTab!!.title)
                            }
                        },
                        onMenuAction = { action ->
                            when (action) {
                                Share -> {
                                    showShareDialog = true
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                MainContent(
                    paddingValues = paddingValues,
                    openTabs = openTabs,
                    editorViewModel = editorViewModel,
                    activeTab = activeTab,
                    jbFontFamily = jbFontFamily,
                    registry = registry,
                    onOpenProjectClick = {
                        directoryPicker.launch(null)
                    },
                    onNewFileClick = {
                        newFileLauncher.launch("untitled.txt")
                    }
                )
            }
        }
    )

    if (showShareDialog) {
        HomeShareDialogs(
            activeTab = activeTab,
            registry = registry,
            context = context,
            scope = scope,
            toastHostState = toastHostState,
            onDismiss = { showShareDialog = false }
        )
    }

    editorUiState.pendingCloseTabId?.let { tabId ->
        val tab = openTabs.find { it.id == tabId } as? WorkspaceTab.TextFile
        if (tab != null) {
            CloseUnsavedTabDialog(
                fileName = tab.title,
                onDismiss = { editorViewModel.dismissCloseTab() },
                onSaveAndClose = { editorViewModel.confirmCloseTab(tabId) },
                onDiscard = { editorViewModel.discardCloseTab(tabId) }
            )
        }
    }

    var nodeToDelete by remember { mutableStateOf<FileNode?>(null) }
    var nodeToRename by remember { mutableStateOf<FileNode?>(null) }
    var nodeToCreateFile by remember { mutableStateOf<FileNode?>(null) }
    var nodeToCreateFolder by remember { mutableStateOf<FileNode?>(null) }

    selectedNodeForAction?.let { node ->
        FileNodeActionSheet(
            node = node,
            isProject = fileTreeViewModel.isRootNode(node),
            fileTreeViewModel = fileTreeViewModel,
            editorViewModel = editorViewModel,
            scope = scope,
            clipboard = clipboard,
            onDismissRequest = { selectedNodeForAction = null },
            onDeleteNode = { nodeToDelete = it },
            onRenameNode = { nodeToRename = it },
            onCreateFile = { nodeToCreateFile = it },
            onCreateFolder = { nodeToCreateFolder = it },
        )
    }

    nodeToRename?.let { targetNode ->
        RenameFileDialog(
            file = targetNode.file,
            onDismiss = { nodeToRename = null },
            onConfirm = { newName ->
                fileTreeViewModel.renameNode(
                    node = targetNode,
                    newName = newName,
                    onSuccess = { newUri ->
                        editorViewModel.handleFileRenamed(targetNode.uri, newUri)
                        nodeToRename = null
                    },
                    onError = { errorMessage ->
                        nodeToRename = null
                        scope.launch {
                            toastHostState.showFailureToast(errorMessage)
                        }
                    }
                )
            }
        )
    }

    nodeToDelete?.let { targetNode ->
        val useTrash = LocalAppSettings.current.fileTree.useTrash
        DeleteFileDialog(
            file = targetNode.file,
            useTrash = useTrash,
            onDismiss = { nodeToDelete = null },
            onMoveToTrash = {
                fileTreeViewModel.deleteNode(
                    node = targetNode,
                    toTrash = true,
                    onSuccess = {
                        editorViewModel.handleFileDeleted(targetNode.uri)
                        nodeToDelete = null
                    },
                    onError = { errorMessage ->
                        nodeToDelete = null
                        scope.launch {
                            toastHostState.showFailureToast(errorMessage)
                        }
                    }
                )
            },
            onDeletePermanently = {
                fileTreeViewModel.deleteNode(
                    node = targetNode,
                    toTrash = false,
                    onSuccess = {
                        editorViewModel.handleFileDeleted(targetNode.uri)
                        nodeToDelete = null
                    },
                    onError = { errorMessage ->
                        nodeToDelete = null
                        scope.launch {
                            toastHostState.showFailureToast(errorMessage)
                        }
                    }
                )
            }
        )
    }

    nodeToCreateFile?.let { targetNode ->
        NewFileDialog(
            onDismiss = { nodeToCreateFile = null },
            onConfirm = { fileName ->
                fileTreeViewModel.createFile(
                    parent = targetNode,
                    fileName = fileName,
                    onSuccess = { newKxFile ->
                        nodeToCreateFile = null
                        editorViewModel.openFile(newKxFile.uri)
                    },
                    onError = { errorMessage ->
                        nodeToCreateFile = null
                        scope.launch {
                            toastHostState.showFailureToast(errorMessage)
                        }
                    }
                )
            }
        )
    }

    nodeToCreateFolder?.let { targetNode ->
        NewFolderDialog(
            onDismiss = { nodeToCreateFolder = null },
            onConfirm = { folderName ->
                fileTreeViewModel.createFolder(
                    parent = targetNode,
                    folderName = folderName,
                    onSuccess = { nodeToCreateFolder = null },
                    onError = { errorMessage ->
                        nodeToCreateFolder = null
                        scope.launch {
                            toastHostState.showFailureToast(errorMessage)
                        }
                    }
                )
            }
        )
    }
}
