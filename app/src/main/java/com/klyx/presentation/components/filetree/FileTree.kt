package com.klyx.presentation.components.filetree

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.WavyProgressIndicatorDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.klyx.R
import com.klyx.data.file.KxFile
import com.klyx.presentation.viewmodel.FileTreeUiState
import com.klyx.presentation.viewmodel.FileTreeViewModel
import com.klyx.ui.animation.orSnap

object FileTree {
    @Composable
    fun drawerWidth(): Dp {
        val containerSize = LocalWindowInfo.current.containerDpSize
        return remember { containerSize.width * 0.77f }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FileTree(
    viewModel: FileTreeViewModel,
    modifier: Modifier = Modifier,
    onNodeClick: (node: FileNode, rootNode: FileNode) -> Unit = { _, _ -> },
    onNodeLongClick: (node: FileNode, rootNode: FileNode) -> Unit = { _, _ -> }
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val visibleNodes by viewModel.visibleNodes.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        modifier = modifier,
        state = pullToRefreshState,
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.refreshTree() },
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                state = pullToRefreshState,
                isRefreshing = state.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .horizontalScroll(rememberScrollState())
        ) {
            if (state.isRefreshing) {
                CircularWavyProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        visibleNodes,
                        key = { it.node.uri.toString() }
                    ) { item ->
                        FileTreeItem(
                            modifier = Modifier.animateItem(
                                fadeInSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow).orSnap(),
                                placementSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    visibilityThreshold = IntOffset.VisibilityThreshold,
                                ).orSnap(),
                                fadeOutSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow).orSnap()
                            ),
                            node = item.node,
                            depth = item.depth,
                            state = state,
                            onClick = { node ->
                                if (node.isDirectory) {
                                    viewModel.toggleNode(node)
                                } else {
                                    onNodeClick(node, item.rootNode)
                                }

                                viewModel.selectNode(node)
                            },
                            onLongClick = { node ->
                                viewModel.selectNode(node)
                                onNodeLongClick(node, item.rootNode)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun FileTreeItem(
    node: FileNode,
    depth: Int,
    state: FileTreeUiState,
    onClick: (FileNode) -> Unit,
    onLongClick: (FileNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isExpanded = state.isNodeExpanded(node)
    val isSelected = state.isNodeSelected(node)
    val isLoading = state.isLoading(node)

    val bg by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "bg_color"
    )

    val horizontalShift by animateDpAsState(
        targetValue = if (isSelected) 4.dp else 0.dp,
        animationSpec = spring<Dp>(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ).orSnap(),
        label = "selection_shift"
    )

    Column(
        modifier = modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = FileTree.drawerWidth())
    ) {
        val shape = MaterialTheme.shapes.extraSmall

        Surface(
            shape = shape,
            color = bg,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .clip(shape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(),
                    onClick = { onClick(node) },
                    onLongClick = { onLongClick(node) }
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                            event.changes.forEach { it.consume() }
                            onLongClick(node)
                        }
                    }
                }
        ) {
            val fg by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                animationSpec = spring<Color>(stiffness = Spring.StiffnessMediumLow).orSnap(),
                label = "fg_color"
            )

            val startPadding = ((depth * 16).dp + horizontalShift).coerceAtLeast(0.dp)

            Row(
                modifier = Modifier
                    .padding(vertical = 6.dp, horizontal = 12.dp)
                    .padding(start = startPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (node.isDirectory) {
                    val rotationDegree by animateFloatAsState(
                        targetValue = if (!isExpanded) 0f else 90f,
                        animationSpec = spring<Float>(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ).orSnap(),
                        label = "rotation"
                    )

                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val stroke = Stroke(
                            width = with(LocalDensity.current) { 2.dp.toPx() },
                            cap = StrokeCap.Round
                        )

                        if (isLoading) {
                            CircularWavyProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                stroke = stroke,
                                trackStroke = stroke,
                                trackColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                } else {
                                    WavyProgressIndicatorDefaults.trackColor
                                },
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                Icons.Rounded.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(18.dp)
                                    .rotate(rotationDegree),
                                tint = fg
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    Spacer(modifier = Modifier.width(24.dp))
                }

                val fileIcon = iconForFile(node.file)
                Icon(
                    painter = fileIcon.painter,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (isSelected) {
                        fg
                    } else if (node.isDirectory) {
                        MaterialTheme.colorScheme.primary
                    } else if (fileIcon.tint.isSpecified) {
                        fileIcon.tint
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = node.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    fontWeight = if (node.isDirectory) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

data class FileIcon(val painter: Painter, val tint: Color = Color.Unspecified)

@Composable
fun iconForFile(file: KxFile): FileIcon {
    if (file.isDirectory) {
        return FileIcon(painterResource(R.drawable.folder_24px))
    }

    if (file.canExecute) {
        return FileIcon(painterResource(R.drawable.terminal_2_24px))
    }

    val name = file.name.lowercase()

    when (name) {
        "license",
        "license.md" -> return FileIcon(painterResource(R.drawable.license_24px))

        "copying" -> return FileIcon(painterResource(R.drawable.copyright_24px))

        ".gitattributes",
        ".editorconfig",
        "readme",
        "readme.txt" -> return FileIcon(painterResource(R.drawable.description_24px))

        "makefile",
        "cmakelists.txt" -> return FileIcon(rememberVectorPainter(Icons.Rounded.Code))

        ".gitignore" -> return FileIcon(rememberVectorPainter(Icons.Rounded.Block))
        ".env" -> return FileIcon(painterResource(R.drawable.key_24px))
    }

    val extension = file.extension.lowercase()

    return when (extension) {
        "kt", "kts" -> FileIcon(painterResource(R.drawable.language_kotlin))
        "java" -> FileIcon(painterResource(R.drawable.language_java))
        "c", "h" -> FileIcon(painterResource(R.drawable.language_c))
        "cpp", "cc", "cxx", "hpp", "hh", "hxx" -> FileIcon(painterResource(R.drawable.language_cpp))

        "cs" -> FileIcon(painterResource(R.drawable.language_csharp))
        "go" -> FileIcon(painterResource(R.drawable.language_go))
        "lua" -> FileIcon(painterResource(R.drawable.language_lua))
        "js", "mjs", "cjs" -> FileIcon(painterResource(R.drawable.language_javascript))
        "ts", "mts", "cts" -> FileIcon(painterResource(R.drawable.language_typescript))
        "py", "pyw" -> FileIcon(painterResource(R.drawable.language_python))
        "php" -> FileIcon(painterResource(R.drawable.language_php))
        "rs" -> FileIcon(painterResource(R.drawable.language_rust))
        "swift" -> FileIcon(painterResource(R.drawable.language_swift))
        "sh", "bash", "zsh", "fish" -> FileIcon(painterResource(R.drawable.terminal_2_24px))

        "html", "htm" -> FileIcon(painterResource(R.drawable.language_html5))
        "css" -> FileIcon(painterResource(R.drawable.language_css3))
        "xml" -> FileIcon(painterResource(R.drawable.code_xml_24px))
        "json", "jsonc" -> FileIcon(painterResource(R.drawable.code_json))

        "md", "mdx", "rst" -> FileIcon(painterResource(R.drawable.markdown_24px))

        "pdf" -> FileIcon(painterResource(R.drawable.pdf_box))

        "png", "jpg", "jpeg", "gif",
        "webp", "bmp", "svg", "ico",
        "avif", "heic", "heif" -> FileIcon(painterResource(R.drawable.image_24px))

        "mp4", "mkv", "webm", "avi",
        "mov", "m4v", "3gp", "wmv",
        "flv", /* "ts" */
            -> FileIcon(painterResource(R.drawable.video_file_24px))

        "mp3", "wav", "ogg", "opus",
        "flac", "aac", "m4a", "wma",
        "amr" -> FileIcon(painterResource(R.drawable.audio_file_24px))

        "zip", "7z", "rar", "tar",
        "gz", "xz", "bz2", "tgz",
        "apk", "jar", "aar" -> FileIcon(painterResource(R.drawable.archive_24px))

        "ttf", "otf", "woff", "woff2" -> FileIcon(painterResource(R.drawable.font_download_24px))
        "txt" -> FileIcon(painterResource(R.drawable.description_24px))
        else -> FileIcon(painterResource(R.drawable.draft_24px))
    }
}
