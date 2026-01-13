package com.klyx.filetree

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import coil3.ImageLoader
import coil3.compose.LocalPlatformContext
import coil3.svg.SvgDecoder
import com.klyx.core.LocalNotifier
import com.klyx.core.file.KxFile
import com.klyx.core.file.Worktree
import com.klyx.core.file.resolve
import com.klyx.di.LocalFileTreeViewModel
import com.klyx.icons.Icons
import com.klyx.icons.KeyboardArrowRight
import com.klyx.ui.component.menu.PopupMenu
import com.klyx.ui.page.main.worktreeDrawerWidth

private inline fun <K, V> LazyListScope.items(
    items: Map<K, V>,
    noinline key: ((item: Map.Entry<K, V>) -> Any)? = null,
    noinline contentType: (item: Map.Entry<K, V>) -> Any? = { null },
    crossinline itemContent: @Composable LazyItemScope.(item: Map.Entry<K, V>) -> Unit
) = items(
    count = items.size,
    key = if (key != null) { index: Int -> key(items.entries.elementAt(index)) } else null,
    contentType = { index: Int -> contentType(items.entries.elementAt(index)) }
) {
    itemContent(items.entries.elementAt(it))
}

private val SvgImageLoader
    @Composable
    @ReadOnlyComposable
    get() = ImageLoader
        .Builder(LocalPlatformContext.current)
        .components { add(SvgDecoder.Factory()) }
        .build()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileTree(
    rootNodes: Map<Worktree, FileTreeNode> = emptyMap(),
    modifier: Modifier = Modifier,
    viewModel: FileTreeViewModel = LocalFileTreeViewModel.current,
    onFileClick: (KxFile, Worktree) -> Unit = { _, _ -> },
    onFileLongClick: (KxFile, Worktree) -> Unit = { _, _ -> },
) {
    LaunchedEffect(rootNodes) {
        viewModel.updateRootNodes(rootNodes)
    }

    val nodes by viewModel.rootNodes.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
    ) {
        val ptrState = rememberPullToRefreshState()

        PullToRefreshBox(
            state = ptrState,
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refreshTree() },
            indicator = {
                PullToRefreshDefaults.IndicatorBox(
                    state = ptrState,
                    modifier = Modifier.align(Alignment.TopCenter),
                    isRefreshing = isRefreshing,
                    maxDistance = PullToRefreshDefaults.IndicatorMaxDistance
                ) {
                    ContainedLoadingIndicator()
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        nodes.entries
                            .distinctBy { it.key.rootFile.absolutePath }
                            .associate { it.toPair() },
                        key = { it.key.rootFile.absolutePath }
                    ) { (worktree, node) ->
                        FileTreeItem(
                            viewModel = viewModel,
                            worktree = worktree,
                            node = node,
                            depth = 0,
                            onFileClick = onFileClick,
                            onFileLongClick = onFileLongClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTreeItem(
    node: FileTreeNode,
    worktree: Worktree,
    viewModel: FileTreeViewModel,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    onFileClick: (KxFile, Worktree) -> Unit = { _, _ -> },
    onFileLongClick: (KxFile, Worktree) -> Unit = { _, _ -> }
) {
    val haptics = LocalHapticFeedback.current
    val notifier = LocalNotifier.current

    val isExpanded = viewModel.isNodeExpanded(node)
    val isSelected = viewModel.isNodeSelected(node)
    val isLoading = viewModel.isNodeLoading(node)

    LaunchedEffect(node, isExpanded) {
        if (isExpanded && node.isDirectory) {
            viewModel.loadChildren(node)
        }
    }

    val children by remember(node, isExpanded) {
        derivedStateOf {
            if (node.isDirectory && isExpanded) {
                viewModel.childrenOf(node)
            } else {
                emptyList()
            }
        }
    }

    val backgroundModifier = if (isSelected) {
        Modifier.background(MaterialTheme.colorScheme.primaryContainer)
    } else {
        Modifier
    }

    val fgColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    var showMenu by remember { mutableStateOf(false) }
    var menuPosition by remember { mutableStateOf(IntOffset.Zero) }

    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = worktreeDrawerWidth)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .indication(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple()
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.forEach { e -> e.consume() }
                            onFileLongClick(node.file, worktree)
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)

                            viewModel.selectNode(node)
                            onFileLongClick(node.file, worktree)

                            menuPosition = IntOffset(offset.x.toInt(), offset.y.toInt())
                            showMenu = true
                        },
                        onTap = {
                            if (node.isDirectory) {
                                viewModel.toggleExpandedState(node)
                            } else {
                                onFileClick(node.file, worktree)
                            }

                            viewModel.selectNode(node)
                        }
                    )
                }
                .then(backgroundModifier)
                .padding(
                    start = (depth * 16).dp,
                    top = 4.dp,
                    bottom = 4.dp,
                    end = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (node.isDirectory) {
                val rotationDegree by animateFloatAsState(
                    targetValue = if (!isExpanded) 0f else 90f,
                    label = "rotation"
                )

                Box(
                    modifier = Modifier.size(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(10.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            Icons.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .rotate(rotationDegree),
                            tint = fgColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Spacer(modifier = Modifier.width(24.dp))
            }

            Icon(
                imageVector = if (node.isDirectory) {
                    node.resolveFolderIcon(isExpanded)
                } else {
                    node.resolveFileIcon()
                },
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = fgColor
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = node.name,
                fontSize = 14.sp,
                fontWeight = if (node.isDirectory) FontWeight.Medium else FontWeight.Normal,
                color = fgColor,
                modifier = Modifier.fillMaxWidth()
            )
        }

        AnimatedVisibility(
            visible = isExpanded && node.isDirectory,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .animateEnterExit()
            ) {
                children.fastForEach { child ->
                    key(child.file.absolutePath) {
                        FileTreeItem(
                            modifier = Modifier.fillMaxWidth(),
                            node = child,
                            worktree = worktree,
                            depth = depth + 1,
                            viewModel = viewModel,
                            onFileClick = onFileClick,
                            onFileLongClick = onFileLongClick
                        )
                    }
                }
            }
        }

        var showRenameDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showNewFileDialog by remember { mutableStateOf(false) }
        var showNewFolderDialog by remember { mutableStateOf(false) }

        val menuItems = rememberFileTreeMenuItems(
            node = node,
            worktree = worktree,
            onNewDocument = { isDirectory ->
                if (isDirectory) {
                    showNewFolderDialog = true
                } else {
                    showNewFileDialog = true
                }
            },
            onRename = { showRenameDialog = true },
            onDelete = { showDeleteDialog = true },
            onCopy = { viewModel.copyNode(node) },
            onCut = { viewModel.cutNode(node) },
            onPaste = {
                if (!viewModel.hasClip()) {
                    notifier.toast("Clipboard is empty")
                    return@rememberFileTreeMenuItems
                }

                if (!viewModel.pasteNode(node)) {
                    notifier.toast("Failed to paste")
                }
            },
            onRefresh = { viewModel.refreshNode(node) }
        )

        if (showMenu) {
            PopupMenu(
                items = menuItems,
                position = menuPosition,
                onDismissRequest = {
                    showMenu = false
                    menuPosition = IntOffset.Zero
                }
            )
        }

        FileActionDialog(
            show = showRenameDialog,
            onDismiss = { showRenameDialog = false },
            title = "Rename",
            confirmLabel = "Rename",
            initialValue = node.name,
            inputLabel = "New name",
            onConfirm = {
                if (node.file.parentFile == null) return@FileActionDialog true
                val success = viewModel.renameNode(node, node.file.parentFile!!.resolve(it))
                if (!success) notifier.toast("Failed to rename file")
                success
            }
        )

        FileActionDialog(
            show = showDeleteDialog,
            onDismiss = { showDeleteDialog = false },
            title = "Delete ${if (node.isDirectory) "Folder" else "File"}",
            confirmLabel = "Delete",
            message = "Are you sure you want to delete \"${node.name}\"? This action cannot be undone.",
            onConfirm = {
                val success = viewModel.deleteNode(node)
                if (!success) notifier.toast("Failed to delete file")
                success
            }
        )

        FileActionDialog(
            show = showNewFileDialog,
            onDismiss = { showNewFileDialog = false },
            title = "New File",
            confirmLabel = "Create",
            inputLabel = "File name",
            onConfirm = {
                val success = viewModel.createNewFile(node, it)
                if (!success) notifier.toast("Failed to create file")
                success
            }
        )

        FileActionDialog(
            show = showNewFolderDialog,
            onDismiss = { showNewFolderDialog = false },
            title = "New Folder",
            confirmLabel = "Create",
            inputLabel = "Folder name",
            onConfirm = {
                val success = viewModel.createNewFolder(node, it)
                if (!success) notifier.toast("Failed to create folder")
                success
            }
        )
    }
}
