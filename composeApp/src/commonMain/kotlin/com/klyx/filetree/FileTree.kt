package com.klyx.filetree

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.svg.SvgDecoder
import com.klyx.DrawerWidth
import com.klyx.core.file.KxFile
import com.klyx.extension.api.Worktree
import org.koin.compose.viewmodel.koinViewModel

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

@Composable
fun FileTree(
    rootNodes: Map<Worktree, FileTreeNode> = emptyMap(),
    modifier: Modifier = Modifier,
    viewModel: FileTreeViewModel = koinViewModel(),
    onFileClick: (KxFile, Worktree) -> Unit = { _, _ -> },
    onFileLongClick: (KxFile, Worktree) -> Unit = { _, _ -> },
) {
    LaunchedEffect(rootNodes) {
        viewModel.updateRootNodes(rootNodes)
    }

    val nodes by viewModel.rootNodes.collectAsState()

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
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

    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = DrawerWidth)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (node.isDirectory) {
                            viewModel.toggleExpandedState(node)
                        } else {
                            onFileClick(node.file, worktree)
                        }

                        viewModel.selectNode(node)
                    },
                    onLongClick = {
                        viewModel.selectNode(node)
                        onFileLongClick(node.file, worktree)
                    }
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
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
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
                imageVector = getFileIcon(node),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = fgColor
            )

//            AsyncImage(
//                "",
//                contentDescription = null,
//                imageLoader = SvgImageLoader
//            )

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
            Column(modifier = Modifier.width(IntrinsicSize.Max)) {
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
    }
}

private fun getFileIcon(node: FileTreeNode): ImageVector {
    if (node.file.isDirectory) {
        return Icons.Default.Folder
    }

    return when (node.file.name.substringAfterLast('.', "").lowercase()) {
        "kt", "kts" -> Icons.Default.Code
        "java" -> Icons.Default.Code
        "js", "ts", "jsx", "tsx" -> Icons.Default.Code
        "py" -> Icons.Default.Code
        "cpp", "c", "h", "hpp" -> Icons.Default.Code
        "cs" -> Icons.Default.Code
        "go" -> Icons.Default.Code
        "rs" -> Icons.Default.Code
        "swift" -> Icons.Default.Code
        "php" -> Icons.Default.Code
        "rb" -> Icons.Default.Code
        "html", "htm" -> Icons.Default.Language
        "css", "scss", "sass" -> Icons.Default.Palette
        "json", "xml", "yaml", "yml" -> Icons.Default.Settings
        "md", "txt" -> Icons.Default.Description
        "png", "jpg", "jpeg", "gif", "svg", "webp" -> Icons.Default.Image
        "pdf" -> Icons.Default.PictureAsPdf
        "zip", "rar", "7z", "tar", "gz" -> Icons.Default.Archive
        "mp4", "avi", "mov", "mkv" -> Icons.Default.VideoFile
        "mp3", "wav", "flac", "ogg" -> Icons.Default.AudioFile
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
