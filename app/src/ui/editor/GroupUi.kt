package com.klyx.ui.editor

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledDropdownMenu
import com.composeunstyled.theme.Theme
import com.klyx.core.action.action
import com.klyx.runtime.action.ActivateDocument
import com.klyx.runtime.action.ActivateEditorGroup
import com.klyx.runtime.action.CloseDocument
import com.klyx.runtime.action.CloseEditorGroup
import com.klyx.runtime.action.PinDocument
import com.klyx.runtime.action.PromotePreviewDocument
import com.klyx.runtime.action.SplitEditor
import com.klyx.runtime.action.UnpinDocument
import com.klyx.runtime.settings.SettingsStore
import com.klyx.runtime.workspace.EditorGroup
import com.klyx.runtime.workspace.EditorGroupId
import com.klyx.runtime.workspace.EditorTabScope
import com.klyx.runtime.workspace.Workspace
import com.klyx.runtime.workspace.WorkspaceDocument
import com.klyx.ui.components.KxMenuItem
import com.klyx.ui.components.KxMenuPanel
import com.klyx.ui.theme.accent
import com.klyx.ui.theme.background
import com.klyx.ui.theme.border
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.kxSmallShape
import com.klyx.ui.theme.label
import com.klyx.ui.theme.labelSmall
import com.klyx.ui.theme.success
import com.klyx.ui.theme.surfaceVariant
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.textSecondary
import com.klyx.ui.theme.typography
import org.koin.compose.koinInject

@Composable
fun rememberEditorGroups(): Pair<List<EditorGroup>, EditorGroupId?> {
    val workspace: Workspace = koinInject()
    val groups by workspace.editorGroups.collectAsState()
    val activeId by workspace.activeEditorGroupId.collectAsState()
    return groups to activeId
}

@Composable
fun rememberHasVisibleFiles(): Boolean {
    val workspace: Workspace = koinInject()
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settings.collectAsState()
    val groups by workspace.editorGroups.collectAsState()
    val documents by workspace.documents.collectAsState()
    val activeProjectId by workspace.activeProjectId.collectAsState()
    val tabScope = if (settings.showAllProjectsTabs) EditorTabScope.AllProjects else EditorTabScope.CurrentProject
    return remember(groups, documents, activeProjectId, tabScope) { workspace.hasVisibleTabs(tabScope) }
}

@Composable
fun GroupStrip(
    groups: List<EditorGroup>,
    activeId: EditorGroupId?,
    modifier: Modifier = Modifier,
) {
    if (groups.size <= 1) return

    val hasVisibleFiles = rememberHasVisibleFiles()

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item(key = "label") {
            Text(
                text = "SPLITS",
                style = Theme[typography][labelSmall],
                color = Theme[colors][textMuted],
            )
        }
        items(items = groups, key = { it.id.value }) { group ->
            GroupPill(
                index = groups.indexOf(group),
                group = group,
                selected = group.id == activeId,
            )
        }
        if (hasVisibleFiles) {
            item(key = "split") {
                val shape = kxSmallShape()
                UnstyledButton(
                    onClick = action(SplitEditor),
                    indication = LocalIndication.current,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .clip(shape)
                        .border(1.dp, Theme[colors][border], shape),
                ) {
                    Text(
                        text = "+ split",
                        style = Theme[typography][labelSmall],
                        color = Theme[colors][accent],
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupPill(index: Int, group: EditorGroup, selected: Boolean) {
    var menuOpen by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val visibleCount = rememberVisibleTabs(group).size

    UnstyledDropdownMenu(
        expanded = menuOpen,
        onExpandedChange = { menuOpen = it },
        sideOffset = 4.dp,
        panel = {
            KxMenuPanel {
                KxMenuItem(
                    label = "focus split ${index + 1}",
                    onClick = action(ActivateEditorGroup(group.id)),
                )
                KxMenuItem(
                    label = "close split ${index + 1}",
                    danger = true,
                    onClick = action(CloseEditorGroup(group.id)),
                )
            }
        },
        anchor = {
            val shape = kxSmallShape()
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (selected) Theme[colors][accent].copy(alpha = 0.14f) else Color.Transparent,
                        shape,
                    )
                    .border(
                        1.dp,
                        if (selected) Theme[colors][accent] else Theme[colors][border],
                        shape,
                    )
                    .alpha(if (pressed) 0.7f else 1f)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = action(ActivateEditorGroup(group.id)),
                        onLongClick = { menuOpen = true },
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (visibleCount) {
                        0 -> "Split ${index + 1} - empty"
                        1 -> "Split ${index + 1} - 1 tab"
                        else -> "Split ${index + 1} - $visibleCount tabs"
                    },
                    style = Theme[typography][labelSmall],
                    color = if (selected) Theme[colors][accent] else Theme[colors][textSecondary],
                )
            }
        },
    )
}

@Composable
fun rememberVisibleTabs(group: EditorGroup): List<WorkspaceDocument> {
    val workspace: Workspace = koinInject()
    val settingsStore: SettingsStore = koinInject()
    val settings by settingsStore.settings.collectAsState()
    val documents by workspace.documents.collectAsState()
    val activeProjectId by workspace.activeProjectId.collectAsState()
    val tabScope = if (settings.showAllProjectsTabs) EditorTabScope.AllProjects else EditorTabScope.CurrentProject
    return remember(group, documents, activeProjectId, tabScope) {
        workspace.visibleTabs(group, tabScope)
    }
}

@Composable
fun TabStrip(group: EditorGroup, modifier: Modifier = Modifier) {
    val workspace: Workspace = koinInject()
    val documents by workspace.documents.collectAsState()
    val buffers by workspace.buffers.collectAsState()
    val tabs = rememberVisibleTabs(group)
    if (tabs.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth().background(Theme[colors][surfaceVariant]),
    ) {
        items(tabs, key = { it.documentId.value }) { tab ->
            DocumentTab(
                tab = tab,
                name = documents[tab.documentId]?.name ?: tab.documentId.value,
                isDirty = buffers[tab.documentId]?.isDirty == true,
                isSelected = tab.documentId == group.activeDocumentId,
            )
        }
    }
}

@Composable
private fun DocumentTab(
    tab: WorkspaceDocument,
    name: String,
    isDirty: Boolean,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    var menuOpen by remember { mutableStateOf(false) }
    val pressed by interactionSource.collectIsPressedAsState()

    val hovered by interactionSource.collectIsHoveredAsState()
    val backgroundColor = when {
        isSelected -> lerp(Theme[colors][background], Theme[colors][accent], 0.10f)
        hovered -> lerp(Theme[colors][surfaceVariant], Theme[colors][accent], 0.06f)
        else -> Theme[colors][surfaceVariant]
    }
    val separatorColor = Theme[colors][border].copy(alpha = 0.5f)
    val borderColor =
        if (tab.pinned) Theme[colors][success] else if (isSelected) Theme[colors][accent] else Theme[colors][border]
    val labelColor = if (isSelected) Theme[colors][textPrimary] else Theme[colors][textSecondary]

    UnstyledDropdownMenu(
        expanded = menuOpen,
        onExpandedChange = { menuOpen = it },
        sideOffset = 4.dp,
        modifier = modifier,
        panel = {
            KxMenuPanel {
                KxMenuItem(
                    label = if (tab.pinned) "unpin" else "pin",
                    onClick = action(if (tab.pinned) UnpinDocument(tab.documentId) else PinDocument(tab.documentId)),
                )
                KxMenuItem(
                    label = "close",
                    danger = true,
                    onClick = action(CloseDocument(tab.documentId)),
                )
            }
        },
        anchor = {
            val shape = kxSmallShape()
            Box(
                modifier = Modifier
                    .background(backgroundColor)
                    .drawBehind {
                        drawLine(
                            separatorColor,
                            Offset(size.width, 0f),
                            Offset(size.width, size.height),
                            1.dp.toPx(),
                        )
                        if (isSelected || tab.pinned) {
                            drawLine(borderColor, Offset.Zero, Offset(size.width, 0f), 2.dp.toPx())
                        }
                    }
                    .hoverable(interactionSource)
                    .semantics { selected = isSelected }
                    .defaultMinSize(minHeight = 44.dp)
                    .alpha(if (pressed) 0.7f else 1f)
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        role = Role.Tab,
                        onClick = action(ActivateDocument(tab.documentId)),
                        onDoubleClick = if (tab.preview) action(PromotePreviewDocument(tab.documentId)) else null,
                        onLongClick = { menuOpen = true },
                    )
                    .pointerHoverIcon(PointerIcon.Default)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (tab.pinned) {
                        Text(
                            text = "*",
                            style = Theme[typography][label],
                            color = Theme[colors][textMuted],
                        )
                    }
                    val style = if (tab.preview) {
                        Theme[typography][label].copy(fontStyle = FontStyle.Italic)
                    } else {
                        Theme[typography][label]
                    }
                    Text(
                        text = name,
                        style = style,
                        color = labelColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.sizeIn(maxWidth = 200.dp),
                    )
                    if (isDirty) {
                        Text(
                            "•",
                            color = Theme[colors][accent],
                            modifier = Modifier.semantics { contentDescription = "Unsaved changes" },
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "×",
                        style = Theme[typography][label],
                        color = Theme[colors][textPrimary],
                        modifier = Modifier
                            .clip(shape)
                            .semantics { contentDescription = "Close $name" }
                            .clickable(onClick = action(CloseDocument(tab.documentId)))
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                    )
                }
            }
        },
    )
}
