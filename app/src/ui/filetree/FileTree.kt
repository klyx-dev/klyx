package com.klyx.ui.filetree

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.UnstyledDropdownMenu
import com.composeunstyled.UnstyledIcon
import com.composeunstyled.buildModifier
import com.composeunstyled.theme.Theme
import com.klyx.core.LocalWindowSizeClass
import com.klyx.core.icons.MaterialSymbolsChevronRight
import com.klyx.core.isWidthAtLeastMediumBreakpoint
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.components.KxMenuItem
import com.klyx.ui.components.KxMenuPanel
import com.klyx.ui.surface
import com.klyx.ui.textPrimary

val platformFileTreeWidth: Dp
    @Composable
    get() {
        val sizeClass = LocalWindowSizeClass.current
        val deviceWidth = LocalWindowInfo.current.containerDpSize.width
        return if (!sizeClass.isWidthAtLeastMediumBreakpoint()) {
            minOf(deviceWidth * 0.82f, 300.dp)
        } else {
            minOf(deviceWidth * 0.3f, 300.dp)
        }
    }

/** One entry of a file tree row's long-press menu. */
data class FileTreeMenuItem(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun FileTree(
    state: FileTreeState,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    rowHeight: Dp = 30.dp,
    indent: Dp = 14.dp,
    iconSize: Dp = 15.dp,
    iconResolver: FileIconResolver = DefaultFileIconRegistry,
    contentSniffer: ContentSniffingIconResolver? = null,
    onFileClick: (VisibleFileNode) -> Unit = {},
    onDoubleClick: (VisibleFileNode) -> Unit = {},
    menuItems: (VisibleFileNode) -> List<FileTreeMenuItem> = { emptyList() },
) {
    var menuNodeId by remember { mutableStateOf<String?>(null) }

    Box(modifier = modifier.horizontalScroll(rememberScrollState())) {
        LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
            items(
                items = state.visibleNodes,
                key = { it.node.id },
                contentType = { if (it.node.isPlaceholder) "placeholder" else if (it.node.isDirectory) "dir" else "file" },
            ) { vn ->
                if (vn.node.isPlaceholder) {
                    ShimmerFileTreeRow(depth = vn.depth, indent = indent, rowHeight = rowHeight, iconSize = iconSize)
                } else {
                    val items = menuItems(vn)
                    FileTreeRow(
                        node = vn,
                        isSelected = state.selectedId == vn.node.id,
                        isExpanded = state.isExpanded(vn.node.id),
                        isLoading = state.isLoading(vn.node.id),
                        indent = indent,
                        rowHeight = rowHeight,
                        iconSize = iconSize,
                        iconResolver = iconResolver,
                        contentSniffer = contentSniffer,
                        onToggle = { state.toggle(vn) },
                        onClick = {
                            state.select(vn.node.id)
                            if (vn.node.isDirectory) state.toggle(vn) else onFileClick(vn)
                        },
                        onDoubleClick = { onDoubleClick(vn) },
                        onLongClick = { if (items.isNotEmpty()) menuNodeId = vn.node.id },
                        menuExpanded = menuNodeId == vn.node.id,
                        onMenuDismiss = { menuNodeId = null },
                        menuContent = {
                            items.forEach { item ->
                                KxMenuItem(
                                    label = item.label,
                                    onClick = {
                                        menuNodeId = null
                                        item.onClick()
                                    },
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileTreeRow(
    node: VisibleFileNode,
    isSelected: Boolean,
    isExpanded: Boolean,
    isLoading: Boolean,
    indent: Dp,
    rowHeight: Dp,
    iconSize: Dp = 16.dp,
    iconResolver: FileIconResolver = DefaultFileIconRegistry,
    contentSniffer: ContentSniffingIconResolver? = null,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onDoubleClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    menuExpanded: Boolean = false,
    onMenuDismiss: () -> Unit = {},
    menuContent: @Composable () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Directories keep the plain clickable path on purpose. combinedClickable has to arbitrate between
    // tap, double tap and long press before it fires onClick, so putting the expand-toggle behind it makes
    // expansion wait out a gesture window that only file rows ever use.
    val gestureModifier = if (node.node.isDirectory) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
            onClickLabel = node.node.name,
        )
    } else {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            onClick = onClick,
            onClickLabel = node.node.name,
            onDoubleClick = onDoubleClick,
            onLongClick = onLongClick,
        )
    }

    UnstyledDropdownMenu(
        expanded = menuExpanded,
        onExpandedChange = { if (!it) onMenuDismiss() },
        sideOffset = 4.dp,
        panel = {
            KxMenuPanel {
                menuContent()
            }
        },
        anchor = {
            Row(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .widthIn(min = platformFileTreeWidth)
                    .height(rowHeight)
                    .padding(horizontal = 2.dp)
                    .clip(RectangleShape)
                    .then(buildModifier {
                        if (isSelected) {
                            add(Modifier.background(Theme[colors][surface]))
                        }
                        if (menuExpanded) {
                            add(Modifier.border(1.dp, Theme[colors][border], RectangleShape))
                        }
                    })
                    .then(gestureModifier)
                    .padding(start = indent * node.depth, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            enabled = node.node.isDirectory,
                            onClick = onToggle,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (node.node.isDirectory) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(10.dp),
                                strokeWidth = 2.dp,
                                color = Theme[colors][textPrimary],
                            )
                        } else {
                            val rotation by animateFloatAsState(if (isExpanded) 90f else 0f)
                            UnstyledIcon(
                                imageVector = MaterialSymbolsChevronRight,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(15.dp)
                                    .rotate(rotation),
                                tint = Theme[colors][textPrimary],
                            )
                        }
                    }
                }

                Spacer(Modifier.width(2.dp))

                FileTreeRowIcon(
                    node = node.node,
                    isExpanded = isExpanded,
                    iconResolver = iconResolver,
                    contentSniffer = contentSniffer,
                    iconSize = iconSize,
                    tint = Theme[colors][textPrimary],
                )

                Spacer(Modifier.width(6.dp))

                Text(text = node.node.name)
            }
        },
    )
}

@Composable
private fun ShimmerFileTreeRow(
    depth: Int,
    indent: Dp,
    rowHeight: Dp,
    iconSize: Dp = 16.dp,
) {
    Row(
        modifier = Modifier
            .width(IntrinsicSize.Max)
            .widthIn(min = 300.dp)
            .height(rowHeight)
            .padding(start = indent * depth, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.size(16.dp)) // arrow slot
        Spacer(Modifier.width(2.dp))
        Box(
            Modifier
                .size(iconSize)
                .clip(RectangleShape)
                .shimmer()
        )
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier
                .height(10.dp)
                .width(110.dp)
                .clip(RectangleShape)
                .shimmer()
        )
    }
}

@Composable
internal fun Modifier.shimmer(): Modifier {
    val highlight = Theme[colors][textPrimary]
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmerAlpha",
    )
    return this.background(highlight.copy(alpha = alpha))
}

@Composable
private fun FileTreeRowIcon(
    node: FileNode,
    isExpanded: Boolean,
    iconResolver: FileIconResolver,
    contentSniffer: ContentSniffingIconResolver?,
    iconSize: Dp,
    tint: Color,
) {
    if (contentSniffer != null) {
        SniffFileContentEffect(node, contentSniffer)
    }

    val icon = iconResolver.resolve(node, isExpanded)

    Box(
        modifier = Modifier.size(iconSize),
        contentAlignment = Alignment.Center,
    ) {
        FileIconView(
            icon = icon ?: FileIcon.Glyph("..."),
            modifier = Modifier.size(iconSize),
            tint = tint,
        )
    }
}
