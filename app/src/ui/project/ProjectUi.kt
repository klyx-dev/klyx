package com.klyx.ui.project

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.UnstyledButton
import com.composeunstyled.UnstyledDropdownMenu
import com.composeunstyled.theme.Theme
import com.klyx.core.action.action
import com.klyx.runtime.action.ActivateProject
import com.klyx.runtime.action.CloseProject
import com.klyx.runtime.action.OpenProject
import com.klyx.runtime.fs.systemHomeDirectory
import com.klyx.runtime.workspace.Project
import com.klyx.runtime.workspace.ProjectId
import com.klyx.runtime.workspace.Workspace
import com.klyx.ui.bodySmall
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.components.KxMenuItem
import com.klyx.ui.components.KxMenuPanel
import com.klyx.ui.components.KxMenuSeparator
import com.klyx.ui.components.KxTextField
import com.klyx.ui.label
import com.klyx.ui.labelSmall
import com.klyx.ui.surfaceVariant
import com.klyx.ui.textMuted
import com.klyx.ui.textPrimary
import com.klyx.ui.typography
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.koin.compose.koinInject

@Composable
fun rememberProjects(): Pair<Map<ProjectId, Project>, List<ProjectId>> {
    val workspace: Workspace = koinInject()
    val all by workspace.projects.collectAsState()
    val inWorkspace by workspace.projectIds.collectAsState()
    return all to inWorkspace
}

@Composable
fun rememberActiveProject(): Project? {
    val workspace: Workspace = koinInject()
    val all by workspace.projects.collectAsState()
    val activeId by workspace.activeProjectId.collectAsState()
    return activeId?.let { all[it] }
}

@Composable
fun ProjectSwitcher(
    onOpenDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    (val all = first, val inWorkspace = second) = rememberProjects()

    val active = rememberActiveProject()
    var expanded by remember { mutableStateOf(false) }

    val ordered = remember(all, inWorkspace) {
        inWorkspace.mapNotNull { id -> all[id] } + (all.values.filter { project -> project.id !in inWorkspace })
    }

    UnstyledDropdownMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        sideOffset = 4.dp,
        modifier = modifier,
        panel = {
            KxMenuPanel {
                if (ordered.isEmpty()) {
                    Text(
                        text = "no projects open",
                        style = Theme[typography][bodySmall],
                        color = Theme[colors][textMuted],
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                } else {
                    ordered.forEach { (val id, val name) ->
                        val isActive = id == active?.id
                        KxMenuItem(
                            label = (if (isActive) "● " else "○ ") + name,
                            onClick = { action(ActivateProject(id)) },
                        )
                    }
                }
                KxMenuSeparator()
                KxMenuItem(label = "open project", onClick = onOpenDialog)
                if (active != null) {
                    KxMenuItem(
                        label = "close ${active.name}",
                        danger = true,
                        onClick = { action(CloseProject(active.id)) },
                    )
                }
            }
        },
        anchor = {
            UnstyledButton(
                onClick = { expanded = true },
                indication = LocalIndication.current,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .clip(RectangleShape)
                    .border(1.dp, Theme[colors][border], RectangleShape)
                    .background(Theme[colors][surfaceVariant], RectangleShape),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 2.dp),
                ) {
                    Text(
                        text = active?.name ?: "no project",
                        style = Theme[typography][label],
                        color = Theme[colors][textPrimary],
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (expanded) "▲" else "▼",
                        style = Theme[typography][labelSmall],
                        color = Theme[colors][textMuted],
                    )
                }
            }
        },
    )
}

@Composable
fun OpenProjectDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val defaultRoot = remember { systemHomeDirectory.toString() }
    val field = rememberTextFieldState(defaultRoot)
    var error by remember { mutableStateOf<String?>(null) }
    var browseOpen by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            error = null
            field.edit { replace(0, length, defaultRoot) }
        }
    }

    KxDialog(
        visible = visible,
        onDismissRequest = onDismiss,
        title = "Open project",
        subtitle = "Enter a folder path. It is added below and selected.",
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxButton(
                    onClick = onDismiss,
                    variant = KxButtonVariant.Ghost,
                    bracketed = true,
                ) {
                    Text(text = "cancel", style = Theme[typography][label])
                }
                KxButton(
                    onClick = {
                        val raw = field.text.toString().trim()
                        if (raw.isEmpty()) {
                            error = "Path is empty"
                            return@KxButton
                        }
                        try {
                            action(OpenProject(raw.toPath()))
                            onDismiss()
                        } catch (t: Throwable) {
                            error = t.message ?: "Invalid path"
                        }
                    },
                    variant = KxButtonVariant.Filled,
                    bracketed = true,
                ) {
                    Text(text = "open", style = Theme[typography][label])
                }
            }
        },
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth(),
        ) {
            KxTextField(
                state = field,
                label = "Folder path",
                placeholder = "/home/you/code/my-app",
                modifier = Modifier.weight(1f),
            )
            KxButton(
                onClick = { browseOpen = true },
                variant = KxButtonVariant.Ghost,
                bracketed = true,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
            ) {
                Text(text = "browse", style = Theme[typography][label])
            }
        }
        if (error != null) {
            Text(
                text = error!!,
                style = Theme[typography][bodySmall],
                color = Theme[colors][com.klyx.ui.error],
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }

    if (browseOpen) {
        FolderPickerDialog(
            initialPath = field.text.toString(),
            onSelect = { chosen ->
                field.edit { replace(0, length, chosen.trim()) }
                browseOpen = false
            },
            onDismiss = { browseOpen = false },
        )
    }
}

@Composable
fun FolderPickerDialog(
    initialPath: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var path by remember { mutableStateOf(initialPath.trim().ifBlank { systemHomeDirectory.toString() }) }
    var entries by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(path) {
        runCatching {
            FileSystem.SYSTEM.list(path.toPath())
                .filter { FileSystem.SYSTEM.metadata(it).isDirectory }
                .map { child -> child.name }
                .sortedBy { it.lowercase() }
        }.onSuccess { result ->
            entries = result
            loadError = null
        }.onFailure { throwable ->
            entries = emptyList()
            loadError = throwable.message ?: "Cannot read folder"
        }
    }

    KxDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = "Select folder",
        subtitle = path,
    ) {
        if (loadError != null) {
            Text(
                text = loadError!!,
                style = Theme[typography][bodySmall],
                color = Theme[colors][com.klyx.ui.error],
                modifier = Modifier.padding(bottom = 8.dp),
            )
        }

        if (path.toPath().parent != null) {
            KxButton(
                onClick = { path = path.toPath().parent?.toString() ?: path },
                variant = KxButtonVariant.Ghost,
                bracketed = true,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                modifier = Modifier.padding(bottom = 8.dp),
            ) {
                Text(text = "..", style = Theme[typography][label])
            }
        }

        LazyColumn(
            modifier = Modifier.heightIn(max = 280.dp),
        ) {
            items(entries) { name ->
                Text(
                    text = name,
                    style = Theme[typography][bodySmall],
                    color = Theme[colors][textPrimary],
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            path = (path.toPath() / name).toString()
                        }
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 10.dp),
        ) {
            KxButton(
                onClick = onDismiss,
                variant = KxButtonVariant.Ghost,
                bracketed = true,
            ) {
                Text(text = "cancel", style = Theme[typography][label])
            }
            KxButton(
                onClick = { onSelect(path) },
                variant = KxButtonVariant.Filled,
            ) {
                Text(text = "select", style = Theme[typography][label])
            }
        }
    }
}
