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
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastDistinctBy
import androidx.compose.ui.util.fastForEach
import com.klyx.DrawerWidth
import com.klyx.core.file.KxFile
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun FileTree(
    rootNodes: List<FileTreeNode>,
    modifier: Modifier = Modifier,
    viewModel: FileTreeViewModel = koinViewModel(),
    onFileClick: (KxFile) -> Unit = {},
    onFileLongClick: (KxFile) -> Unit = {},
) {
    LaunchedEffect(rootNodes) {
        viewModel.updateRootNodes(rootNodes)
        //viewModel.expandNode(rootNodes.first())
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
                    nodes.fastDistinctBy { it.file.absolutePath },
                    key = { it.file.absolutePath }
                ) { node ->
                    FileTreeItem(
                        viewModel = viewModel,
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
    viewModel: FileTreeViewModel,
    modifier: Modifier = Modifier,
    depth: Int = 0,
    onFileClick: (KxFile) -> Unit = {},
    onFileLongClick: (KxFile) -> Unit = {}
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

    val fileNameColor = if (isSelected) {
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
                            onFileClick(node.file)
                        }

                        viewModel.selectNode(node)
                    },
                    onLongClick = {
                        viewModel.selectNode(node)
                        onFileLongClick(node.file)
                    }
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.forEach { e -> e.consume() }
                            onFileLongClick(node.file)
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
                                .rotate(rotationDegree)
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
                tint = getFileIconColor(node)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = node.name,
                fontSize = 14.sp,
                fontWeight = if (node.isDirectory) FontWeight.Medium else FontWeight.Normal,
                color = fileNameColor,
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

@Composable
private fun getFileIconColor(node: FileTreeNode): Color {
    if (node.file.isDirectory) {
        return MaterialTheme.colorScheme.primary
    }

    return when (node.file.name.substringAfterLast('.', "").lowercase()) {
        "kt", "kts" -> Color(0xFF7F52FF)
        "java" -> Color(0xFFED8B00)
        "js", "jsx" -> Color(0xFFF7DF1E)
        "ts", "tsx" -> Color(0xFF3178C6)
        "py" -> Color(0xFF3776AB)
        "cpp", "c", "h", "hpp" -> Color(0xFF00599C)
        "cs" -> Color(0xFF239120)
        "go" -> Color(0xFF00ADD8)
        "rs" -> Color(0xFF000000)
        "swift" -> Color(0xFFFA7343)
        "php" -> Color(0xFF777BB4)
        "rb" -> Color(0xFFCC342D)
        "html", "htm" -> Color(0xFFE34F26)
        "css", "scss", "sass" -> Color(0xFF1572B6)
        "json" -> Color(0xFFFFD700)
        "xml" -> Color(0xFF0060AC)
        "yaml", "yml" -> Color(0xFFCB171E)
        "md" -> Color(0xFF083FA1)
        "png", "jpg", "jpeg", "gif", "svg", "webp" -> Color(0xFF4CAF50)
        "pdf" -> Color(0xFFD32F2F)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
