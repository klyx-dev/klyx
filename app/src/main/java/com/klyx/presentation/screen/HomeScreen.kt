package com.klyx.presentation.screen

import android.content.ClipData
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEachIndexed
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil3.compose.AsyncImage
import com.klyx.R
import com.klyx.api.data.editor.EditorAction
import com.klyx.api.data.editor.Save
import com.klyx.api.data.editor.SaveAs
import com.klyx.api.data.editor.WorkspaceTab
import com.klyx.api.data.file.KxFile
import com.klyx.api.data.file.wrap
import com.klyx.api.data.preferences.LocalAppSettings
import com.klyx.api.ui.LocalToastHostState
import com.klyx.api.ui.ToolbarAction
import com.klyx.api.ui.ToolbarCategory
import com.klyx.api.ui.ToolbarRegistry
import com.klyx.api.ui.showFailureToast
import com.klyx.api.ui.theme.GoogleSansRounded
import com.klyx.api.ui.theme.JetBrainsMonoFontFamily
import com.klyx.api.ui.theme.LocalIsDarkMode
import com.klyx.api.util.share
import com.klyx.api.util.shareText
import com.klyx.core.globalOf
import com.klyx.data.editor.EditorStateRegistry
import com.klyx.data.editor.KlyxEditorColorScheme
import com.klyx.data.editor.applyEditorSettings
import com.klyx.data.file.openWith
import com.klyx.data.file.share
import com.klyx.data.file.shareableUri
import com.klyx.data.preferences.FontManager
import com.klyx.icons.Klyx
import com.klyx.icons.KlyxIcons
import com.klyx.presentation.components.AnimatedTab
import com.klyx.presentation.components.CloseProject
import com.klyx.presentation.components.Copy
import com.klyx.presentation.components.CopyPath
import com.klyx.presentation.components.Cut
import com.klyx.presentation.components.Delete
import com.klyx.presentation.components.ExpressiveMenuItem
import com.klyx.presentation.components.FileActionBottomSheet
import com.klyx.presentation.components.NewDirectory
import com.klyx.presentation.components.NewFile
import com.klyx.presentation.components.OpenWith
import com.klyx.presentation.components.Paste
import com.klyx.presentation.components.Rename
import com.klyx.presentation.components.Share
import com.klyx.presentation.components.UnsupportedFileDialog
import com.klyx.presentation.components.WelcomeScreen
import com.klyx.presentation.components.dialogs.DeleteFileDialog
import com.klyx.presentation.components.dialogs.ImageShareDialog
import com.klyx.presentation.components.dialogs.NewFileDialog
import com.klyx.presentation.components.dialogs.NewFolderDialog
import com.klyx.presentation.components.dialogs.RenameFileDialog
import com.klyx.presentation.components.dialogs.ShareDialog
import com.klyx.presentation.components.filetree.FileNode
import com.klyx.presentation.components.filetree.FileTreeDrawer
import com.klyx.presentation.navigation.LocalNavigator
import com.klyx.presentation.navigation.Screen
import com.klyx.presentation.viewmodel.EditorEvent
import com.klyx.presentation.viewmodel.EditorViewModel
import com.klyx.presentation.viewmodel.FileTreeViewModel
import com.klyx.presentation.viewmodel.HomeViewModel
import com.klyx.ui.provider.LocalTreeSitter
import io.github.rosemoe.sora.compose.CodeEditor
import io.github.rosemoe.sora.compose.CodeEditorState
import io.github.rosemoe.sora.compose.ExperimentalEditorApi
import io.github.rosemoe.sora.compose.content
import io.github.rosemoe.sora.compose.rememberCodeEditorState
import io.github.rosemoe.sora.event.ContentChangeEvent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalLayoutApi::class, ExperimentalEditorApi::class, ExperimentalMaterial3Api::class)
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
                    scope.launch { toastHostState.showFailureToast("Failed to create file") }
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
                containerColor = Color.Transparent,
                topBar = {
                    val toolbarActions = globalOf<ToolbarRegistry>().actions()

                    HomeTopBar(
                        scrollBehavior = scrollBehavior,
                        drawerState = drawerState,
                        scope = scope,
                        openTabs = openTabs,
                        activeTab = activeTab,
                        toolbarActions = toolbarActions,
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
                                MenuAction.Share -> {
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

    if (showShareDialog && activeTab is WorkspaceTab.TextFile) {
        val tab = activeTab as WorkspaceTab.TextFile
        val editorState = registry[tab.id]
        val hasSelection = editorState?.cursor?.isSelected ?: false

        ShareDialog(
            fileName = tab.title,
            hasSelection = hasSelection,
            onDismiss = { showShareDialog = false },
            onShareSelection = {
                showShareDialog = false
                val cursor = editorState?.cursor
                val text = editorState?.text

                if (cursor != null && text != null && hasSelection) {
                    val start = minOf(cursor.left, cursor.right)
                    val end = maxOf(cursor.left, cursor.right)
                    val selectedText = text.substring(start, end)

                    shareText(selectedText)
                } else {
                    scope.launch {
                        toastHostState.showFailureToast("No text selected to share")
                    }
                }
            },
            onShareFileText = {
                scope.launch(Dispatchers.IO) {
                    val wholeText = (activeTab as WorkspaceTab.TextFile).file.readText()
                    withContext(Dispatchers.Main.immediate) {
                        shareText(wholeText)
                    }
                }
                showShareDialog = false
            },
            onShareFile = {
                showShareDialog = false
                (activeTab as WorkspaceTab.TextFile).file.share()
            }
        )
    }

    if (showShareDialog && activeTab is WorkspaceTab.ImageFile) {
        val tab = (activeTab as WorkspaceTab.ImageFile)

        ImageShareDialog(
            fileName = tab.title,
            imageUri = tab.uri,
            onDismiss = { showShareDialog = false },
            onShare = {
                showShareDialog = false
                tab.uri.share()
            }
        )
    }

    var nodeToDelete by remember { mutableStateOf<FileNode?>(null) }
    var nodeToRename by remember { mutableStateOf<FileNode?>(null) }

    var nodeToCreateFile by remember { mutableStateOf<FileNode?>(null) }
    var nodeToCreateFolder by remember { mutableStateOf<FileNode?>(null) }

    selectedNodeForAction?.let { node ->
        val sheetState = rememberBottomSheetState(
            initialValue = Hidden,
            enabledValues = setOf(Hidden, Expanded)
        )

        val dismiss: () -> Unit = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                if (!sheetState.isVisible) {
                    selectedNodeForAction = null
                }
            }
        }

        FileActionBottomSheet(
            file = node.file,
            isProject = fileTreeViewModel.isRootNode(node),
            sheetState = sheetState,
            onDismissRequest = { selectedNodeForAction = null },
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
                        nodeToDelete = node
                        dismiss()
                    }

                    is Rename -> {
                        nodeToRename = node
                        dismiss()
                    }

                    is OpenWith -> action.file.openWith()
                    is Share -> action.file.share()

                    is Paste -> {
                        fileTreeViewModel
                            .visibleNodes
                            .value
                            .firstNotNullOfOrNull {
                                if (it.node.uri == action.destination.uri) {
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
                        nodeToDelete = node
                        dismiss()
                    }

                    is Rename -> {
                        nodeToRename = node
                        dismiss()
                    }

                    is NewFile -> {
                        nodeToCreateFile = node
                        dismiss()
                    }

                    is NewDirectory -> {
                        nodeToCreateFolder = node
                        dismiss()
                    }

                    is Paste -> {
                        fileTreeViewModel
                            .visibleNodes
                            .value
                            .firstNotNullOfOrNull {
                                if (it.node.uri == action.destination.uri) {
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
                        dismiss()
                    }
                }
            }
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
        DeleteFileDialog(
            file = targetNode.file,
            onDismiss = { nodeToDelete = null },
            onConfirm = {
                fileTreeViewModel.deleteNode(
                    node = targetNode,
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

private suspend fun copyPath(file: KxFile, clipboard: Clipboard) {
    clipboard.setClipEntry(ClipData.newPlainText("klyx", file.absolutePath).toClipEntry())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    drawerState: DrawerState,
    scope: CoroutineScope,
    openTabs: ImmutableList<WorkspaceTab>,
    activeTab: WorkspaceTab?,
    toolbarActions: List<ToolbarAction>,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabCloseOthers: (String) -> Unit,
    onTabCloseAll: () -> Unit,
    onAction: (EditorAction) -> Unit,
    onMenuAction: (MenuAction) -> Unit,
    onSaveAsClick: () -> Unit
) {
    val gradientColors = persistentListOf(
        MaterialTheme.colorScheme.primaryContainer,
        Color.Transparent
    )

    val brush = remember(gradientColors) {
        Brush.verticalGradient(colors = gradientColors)
    }

    Column(modifier = Modifier.background(brush)) {
        TopAppBar(
            scrollBehavior = scrollBehavior,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                scrolledContainerColor = Color.Transparent
            ),
            title = {},
            navigationIcon = {
                IconButton(
                    onClick = { scope.launch { drawerState.open() } },
                    shapes = IconButtonDefaults.shapes(
                        shape = MaterialTheme.shapes.medium,
                        pressedShape = MaterialTheme.shapes.small
                    ),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.folder_code_24px),
                        contentDescription = "File Explorer"
                    )
                }
            },
            actions = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (activeTab is WorkspaceTab.TextFile) {
                        FilledIconButton(
                            shapes = IconButtonDefaults.shapes(),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            onClick = { onAction(Save(activeTab.file)) }
                        ) {
                            Icon(
                                Icons.Outlined.Save,
                                contentDescription = "Save File"
                            )
                        }
                    }

                    MainMenu(
                        activeTab = activeTab,
                        toolbarActions = toolbarActions,
                        onAction = onMenuAction,
                        onEditorAction = onAction,
                        onSaveAsClick = onSaveAsClick
                    )
                }
            }
        )

        if (openTabs.isNotEmpty()) {
            EditorTabs(
                openTabs = openTabs,
                activeTab = activeTab,
                onTabClick = onTabClick,
                onTabClose = onTabClose,
                onTabCloseOthers = onTabCloseOthers,
                onTabCloseAll = onTabCloseAll
            )
        }
    }
}

private sealed interface MenuAction {
    data object Share : MenuAction
}

@Composable
private fun MainMenu(
    activeTab: WorkspaceTab?,
    toolbarActions: List<ToolbarAction>,
    onAction: (MenuAction) -> Unit,
    onEditorAction: (EditorAction) -> Unit,
    onSaveAsClick: () -> Unit
) {
    val navigator = LocalNavigator.current
    var showMenu by remember { mutableStateOf(false) }

    val byCategory = toolbarActions.groupBy { it.category }
    val currentFileActions = byCategory[ToolbarCategory.CurrentFile].orEmpty().sortedBy { it.priority }
    val workspaceActions = byCategory[ToolbarCategory.Workspace].orEmpty().sortedBy { it.priority }
    val customCategories = byCategory.keys
        .minus(setOf(ToolbarCategory.CurrentFile, ToolbarCategory.Workspace, ToolbarCategory.Plugins))
        .sorted()
    val pluginsActions = byCategory[ToolbarCategory.Plugins].orEmpty().sortedBy { it.priority }
    val hasCustomOrPlugins = customCategories.isNotEmpty() || pluginsActions.isNotEmpty()

    Box {
        FilledIconButton(
            shapes = IconButtonDefaults.shapes(),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                contentColor = MaterialTheme.colorScheme.onSurface
            ),
            onClick = { showMenu = true }
        ) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = "More Options"
            )
        }

        MaterialTheme(
            shapes = MaterialTheme.shapes.copy(
                extraSmall = RoundedCornerShape(20.dp)
            )
        ) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.widthIn(min = 180.dp)
            ) {
                if (activeTab is WorkspaceTab.TextFile || activeTab is WorkspaceTab.ImageFile) {
                    Text(
                        text = "Current File",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    if (activeTab is WorkspaceTab.TextFile) {
                        ExpressiveMenuItem(
                            text = "Save As...",
                            icon = painterResource(R.drawable.save_as_24px),
                            onClick = {
                                showMenu = false
                                onSaveAsClick()
                            }
                        )
                    }

                    ExpressiveMenuItem(
                        text = "Share",
                        icon = painterResource(R.drawable.share_24px),
                        onClick = {
                            showMenu = false
                            onAction(MenuAction.Share)
                        }
                    )

                    currentFileActions.forEach { action ->
                        ExpressiveMenuItem(
                            text = action.label,
                            icon = action.icon,
                            onClick = {
                                showMenu = false
                                action.onClick()
                            }
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                Text(
                    text = "Workspace",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = GoogleSansRounded,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                ExpressiveMenuItem(
                    text = "Terminal",
                    icon = painterResource(R.drawable.terminal_2_24px),
                    onClick = {
                        showMenu = false
                        navigator.navigateTo(Screen.Terminal)
                    }
                )

                ExpressiveMenuItem(
                    text = "Settings",
                    icon = painterResource(R.drawable.settings_24px),
                    onClick = {
                        showMenu = false
                        navigator.navigateTo(Screen.Settings)
                    }
                )

                workspaceActions.forEach { action ->
                    ExpressiveMenuItem(
                        text = action.label,
                        icon = action.icon,
                        onClick = {
                            showMenu = false
                            action.onClick()
                        }
                    )
                }

                if (hasCustomOrPlugins) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                customCategories.forEach { category ->
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    byCategory[category]!!.sortedBy { it.priority }.forEach { action ->
                        ExpressiveMenuItem(
                            text = action.label,
                            icon = action.icon,
                            onClick = {
                                showMenu = false
                                action.onClick()
                            }
                        )
                    }
                }

                if (pluginsActions.isNotEmpty()) {
                    if (customCategories.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }

                    Text(
                        text = "Plugins",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = GoogleSansRounded,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    pluginsActions.forEach { action ->
                        ExpressiveMenuItem(
                            text = action.label,
                            icon = action.icon,
                            onClick = {
                                showMenu = false
                                action.onClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorTabs(
    openTabs: ImmutableList<WorkspaceTab>,
    activeTab: WorkspaceTab?,
    onTabClick: (String) -> Unit,
    onTabClose: (String) -> Unit,
    onTabCloseOthers: (String) -> Unit,
    onTabCloseAll: () -> Unit
) {
    val activeTabIndex = remember(openTabs, activeTab) {
        val index = openTabs.indexOfFirst { it.id == activeTab?.id }
        if (index != -1) index else 0
    }

    PrimaryScrollableTabRow(
        selectedTabIndex = activeTabIndex,
        containerColor = Color.Transparent,
        edgePadding = 4.dp,
        indicator = {},
        divider = {},
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        val hapticFeedback = LocalHapticFeedback.current

        openTabs.fastForEachIndexed { index, tab ->
            val isActive = index == activeTabIndex
            var isMenuExpanded by remember { mutableStateOf(false) }

            val isModified = tab is WorkspaceTab.TextFile && tab.hasUnsavedChanges

            AnimatedTab(
                index = index,
                selectedIndex = activeTabIndex,
                onClick = {
                    if (isActive) {
                        isMenuExpanded = true
                    } else {
                        onTabClick(tab.id)
                    }
                },
            ) {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(
                                start = 6.dp,
                                top = 2.dp,
                                bottom = 2.dp,
                                end = 2.dp
                            )
                            .animateContentSize()
                    ) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = GoogleSansRounded,
                            fontWeight = if (index == activeTabIndex) FontWeight.SemiBold else FontWeight.Medium
                        )

                        AnimatedVisibility(
                            visible = isModified,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(start = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LocalContentColor.current)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onTabClose(tab.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close ${tab.title}",
                                modifier = Modifier.size(14.dp),
                                tint = LocalContentColor.current
                            )
                        }
                    }

                    MaterialTheme(
                        shapes = MaterialTheme.shapes.copy(
                            extraSmall = RoundedCornerShape(16.dp)
                        )
                    ) {
                        DropdownMenu(
                            expanded = isMenuExpanded,
                            onDismissRequest = { isMenuExpanded = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Close",
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = { onTabClose(tab.id); isMenuExpanded = false },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = null
                                    )
                                }
                            )

                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Close Others",
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Medium
                                    )
                                },
                                onClick = { onTabCloseOthers(tab.id); isMenuExpanded = false },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.CloseFullscreen,
                                        contentDescription = null
                                    )
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Close All",
                                        fontFamily = GoogleSansRounded,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                onClick = { onTabCloseAll(); isMenuExpanded = false },
                                leadingIcon = {
                                    Icon(
                                        Icons.Rounded.DeleteSweep,
                                        contentDescription = null
                                    )
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.error,
                                    leadingIconColor = MaterialTheme.colorScheme.error
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainContent(
    paddingValues: PaddingValues,
    editorViewModel: EditorViewModel,
    openTabs: ImmutableList<WorkspaceTab>,
    activeTab: WorkspaceTab?,
    jbFontFamily: FontFamily,
    registry: EditorStateRegistry,
    onOpenProjectClick: () -> Unit,
    onNewFileClick: () -> Unit
) {
    if (openTabs.isEmpty()) {
        WelcomeScreen(
            onNewFileClick = onNewFileClick,
            onOpenProjectClick = onOpenProjectClick,
            modifier = Modifier.padding(paddingValues)
        )
    } else {
        val pagerState = rememberPagerState(pageCount = { openTabs.size })

        LaunchedEffect(activeTab) {
            val index = openTabs.indexOfFirst { it == activeTab }
            if (index != -1 && index != pagerState.currentPage) {
                pagerState.scrollToPage(index)
            }
        }

        val isDarkMode = LocalIsDarkMode.current
        val colorScheme = MaterialTheme.colorScheme
        val selectionColors = LocalTextSelectionColors.current

        EditorPager(
            pagerState = pagerState,
            openTabs = openTabs,
            paddingValues = paddingValues,
            jbFontFamily = jbFontFamily,
            isDarkMode = isDarkMode,
            colorScheme = colorScheme,
            editorViewModel = editorViewModel,
            selectionColors = selectionColors,
            registry = registry,
            onNewFileClick = onNewFileClick,
            onOpenProjectClick = onOpenProjectClick
        )
    }
}

@Composable
fun EditorEmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = KlyxIcons.Klyx,
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "K L Y X",
                style = MaterialTheme.typography.headlineMedium,
                fontFamily = GoogleSansRounded,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Swipe from the left edge\nto open the explorer",
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = GoogleSansRounded,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun EditorPager(
    pagerState: PagerState,
    openTabs: ImmutableList<WorkspaceTab>,
    paddingValues: PaddingValues,
    jbFontFamily: FontFamily,
    isDarkMode: Boolean,
    colorScheme: ColorScheme,
    selectionColors: TextSelectionColors,
    editorViewModel: EditorViewModel,
    registry: EditorStateRegistry,
    onNewFileClick: () -> Unit,
    onOpenProjectClick: () -> Unit
) {
    HorizontalPager(
        state = pagerState,
        userScrollEnabled = false,
        beyondViewportPageCount = openTabs.size,
        key = { openTabs[it].id },
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .imePadding()
    ) { pageIndex ->
        when (val tab = openTabs[pageIndex]) {
            is WorkspaceTab.TextFile -> {
                TextFileEditor(
                    tab = tab,
                    jbFontFamily = jbFontFamily,
                    isDarkMode = isDarkMode,
                    colorScheme = colorScheme,
                    selectionColors = selectionColors,
                    editorViewModel = editorViewModel,
                    registry = registry
                )
            }

            is WorkspaceTab.ImageFile -> {
                val zoomState = rememberZoomState(maxScale = 100f)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    AsyncImage(
                        model = tab.uri,
                        contentDescription = tab.title,
                        contentScale = ContentScale.Fit,
                        onSuccess = { state ->
                            zoomState.setContentSize(state.painter.intrinsicSize)
                        },
                        filterQuality = FilterQuality.High,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(zoomState = zoomState)
                    )
                }
            }

            is WorkspaceTab.Welcome -> {
                WelcomeScreen(
                    onNewFileClick = onNewFileClick,
                    onOpenProjectClick = onOpenProjectClick
                )
            }

            is WorkspaceTab.Custom -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds()
                ) {
                    tab.content()
                }
            }
        }
    }
}

@Composable
private fun TextFileEditor(
    tab: WorkspaceTab.TextFile,
    jbFontFamily: FontFamily,
    isDarkMode: Boolean,
    colorScheme: ColorScheme,
    selectionColors: TextSelectionColors,
    editorViewModel: EditorViewModel,
    registry: EditorStateRegistry
) {
    val settings = LocalAppSettings.current.editor
    val state = tab.createEditorState()
    val scheme = remember(isDarkMode, colorScheme, selectionColors) {
        KlyxEditorColorScheme(isDarkMode, colorScheme, selectionColors)
    }

    LaunchedEffect(settings) {
        state.applyEditorSettings(settings)
    }

    var isAccessoryBarVisible by remember { mutableStateOf(true) }

    DisposableEffect(tab.id) {
        registry.register(tab.id, state)
        onDispose { registry.unregister(tab.id) }
    }

    LaunchedEffect(scheme, state) {
        state.colorScheme = scheme
    }

    LaunchedEffect(tab.id, state) {
        state.content
            .drop(1)
            .collect { content ->
                editorViewModel.markTabModified(tab.id, content.toString() != tab.text)
            }
    }

    key(tab.id) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                val fontManager: FontManager = koinInject()
                var currentFont by remember { mutableStateOf(JetBrainsMonoFontFamily) }
                LaunchedEffect(settings.customFontUri) {
                    currentFont = fontManager.getFontFamily(settings.customFontUri)
                }

                CodeEditor(
                    state = state,
                    fontFamily = currentFont,
                    fontSize = settings.fontSize.sp,
                    modifier = Modifier.fillMaxSize()
                )

                this@Column.AnimatedVisibility(
                    visible = !isAccessoryBarVisible,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FilledIconButton(
                        onClick = { isAccessoryBarVisible = true },
                        modifier = Modifier.size(44.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            Icons.Rounded.KeyboardArrowUp,
                            contentDescription = "Show Tools",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isAccessoryBarVisible,
                enter = slideInVertically(initialOffsetY = { it }) +
                        expandVertically(expandFrom = Alignment.Bottom) +
                        fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) +
                        shrinkVertically(shrinkTowards = Alignment.Bottom) +
                        fadeOut()
            ) {
                EditorAccessoryBar(
                    state = state,
                    fontFamily = jbFontFamily,
                    onHide = { isAccessoryBarVisible = false }
                )
            }
        }
    }
}

@Composable
private fun EditorAccessoryBar(
    state: CodeEditorState,
    fontFamily: FontFamily,
    onHide: () -> Unit,
    modifier: Modifier = Modifier
) {
    var canUndo by remember { mutableStateOf(state.canUndo) }
    var canRedo by remember { mutableStateOf(state.canRedo) }

    val refreshUndoRedo = {
        canRedo = state.canRedo
        canUndo = state.canUndo
    }

    DisposableEffect(state) {
        refreshUndoRedo()

        val receipt = state.subscribeAlways<ContentChangeEvent> {
            refreshUndoRedo()
        }

        onDispose { receipt.unsubscribe() }
    }

    val symbols = listOf("{", "}", "(", ")", "[", "]", "<", ">", "=", ";", "\"", "'")

    Surface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                IconButton(
                    onClick = { state.undo(); refreshUndoRedo() },
                    enabled = canUndo,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Undo,
                        contentDescription = "Undo",
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = { state.redo(); refreshUndoRedo() },
                    enabled = canRedo,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.Redo,
                        contentDescription = "Redo",
                        modifier = Modifier.size(20.dp)
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(20.dp)
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )

                AccessoryKeyButton(
                    text = "TAB",
                    onClick = {
                        if (state.snippetController.isInSnippet()) {
                            state.snippetController.shiftToNextTabStop()
                        } else {
                            state.indentOrCommitTab()
                        }
                    },
                    fontFamily = fontFamily,
                    isWide = true
                )

                symbols.forEach { symbol ->
                    AccessoryKeyButton(
                        text = symbol,
                        fontFamily = fontFamily,
                        onClick = { state.insertText(symbol, 1) }
                    )
                }
            }

            var isHiding by remember { mutableStateOf(false) }
            val rotationAngle by animateFloatAsState(
                targetValue = if (isHiding) 180f else 0f,
                animationSpec = tween(durationMillis = 300),
                label = "arrow_rotation"
            )

            IconButton(
                onClick = {
                    isHiding = true
                    onHide()
                },
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Hide Toolbar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            rotationZ = rotationAngle
                        }
                )
            }
        }
    }
}

@Composable
private fun AccessoryKeyButton(
    text: String,
    fontFamily: FontFamily,
    onClick: () -> Unit,
    isWide: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .height(36.dp)
            .widthIn(min = if (isWide) 52.dp else 32.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = fontFamily,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalEditorApi::class)
@Composable
private fun WorkspaceTab.TextFile.createEditorState(): CodeEditorState {
    val state = rememberCodeEditorState(initialText = text)

    val density = LocalDensity.current
    val treeSitter = LocalTreeSitter.current

    LaunchedEffect(Unit) {
        state.lineNumberMarginLeft = with(density) { 5.dp.toPx() }
        state.editorLanguage = treeSitter.getLanguageForExtension(file.extension)
    }

    return state
}
