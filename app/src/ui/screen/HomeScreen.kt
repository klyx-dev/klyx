package com.klyx.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.UnstyledDropdownMenu
import com.composeunstyled.theme.Theme
import com.klyx.core.Debug
import com.klyx.core.IoDispatcher
import com.klyx.core.action.action
import com.klyx.editor.EditorStore
import com.klyx.navigation.LocalNavigator
import com.klyx.navigation.Settings
import com.klyx.runtime.action.InsertText
import com.klyx.runtime.action.OpenDocument
import com.klyx.runtime.action.OpenProject
import com.klyx.runtime.action.PromotePreviewDocument
import com.klyx.runtime.action.SplitEditor
import com.klyx.runtime.fs.systemHomeDirectory
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentId
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.EditorGroup
import com.klyx.runtime.workspace.EditorGroupId
import com.klyx.runtime.workspace.EditorTabScope
import com.klyx.runtime.workspace.ProjectId
import com.klyx.runtime.workspace.Workspace
import com.klyx.runtime.workspace.isVisibleIn
import com.klyx.ui.accent
import com.klyx.ui.background
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonDefaultMinHeight
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxMenuItem
import com.klyx.ui.components.KxMenuPanel
import com.klyx.ui.currentFontFamily
import com.klyx.ui.editor.GroupStrip
import com.klyx.ui.editor.TabStrip
import com.klyx.ui.editor.rememberEditorGroups
import com.klyx.ui.editor.rememberHasVisibleFiles
import com.klyx.ui.filetree.ChildrenLoader
import com.klyx.ui.filetree.FileNode
import com.klyx.ui.filetree.FileTree
import com.klyx.ui.filetree.FileTreeMenuItem
import com.klyx.ui.filetree.FileTreeState
import com.klyx.ui.filetree.NodeIdRegistry
import com.klyx.ui.filetree.platformFileTreeWidth
import com.klyx.ui.filetree.rememberFileTreeIcons
import com.klyx.ui.headline
import com.klyx.ui.label
import com.klyx.ui.labelSmall
import com.klyx.ui.project.OpenProjectDialog
import com.klyx.ui.project.ProjectSwitcher
import com.klyx.ui.surface
import com.klyx.ui.textMuted
import com.klyx.ui.textPrimary
import com.klyx.ui.textSecondary
import com.klyx.ui.typography
import io.github.lumkit.sweeteditor.EditorActionSource
import io.github.lumkit.sweeteditor.EditorSettings
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.TextChangeKind
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.koin.compose.koinInject

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var openProjectDialog by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.current

    val workspace: Workspace = koinInject()
    val activeProjectId by workspace.activeProjectId.collectAsState()
    val tabScope by workspace.tabScope.collectAsState()

    Debug {
        if (workspace.projects.value.isEmpty()) {
            try {
                action(OpenProject(systemHomeDirectory))
                val job = action(OpenDocument(systemHomeDirectory / "vivek/settings.json"))
                job.join()
            } catch (_: Throwable) {
                currentCoroutineContext().ensureActive()
            }
        }
    }

    (val groups = first, val activeId = second) = rememberEditorGroups()

    LaunchedEffect(activeProjectId, tabScope) {
        if (tabScope != EditorTabScope.CurrentProject) return@LaunchedEffect
        val documents = workspace.documents.value
        workspace.editorGroups.value.forEach { group ->
            val visible = group.documents.filter { tab ->
                documents[tab.documentId]?.isVisibleIn(activeProjectId) ?: true
            }
            val selectionVisible = group.activeDocumentId?.let { id ->
                visible.any { it.documentId == id }
            } == true
            if (!selectionVisible && visible.isNotEmpty()) {
                workspace.activateDocument(group.id, visible.first().documentId)
            }
        }
    }

    FileTreeDrawer(
        modifier = modifier,
        drawerState = drawerState,
        onOpenDialog = { openProjectDialog = true },
        onNavigate = { scope.launch { drawerState.close() } },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TopBar(
                onFilesClick = { scope.launch { drawerState.open() } },
                onOpenDialog = { openProjectDialog = true },
                onSettingsClick = { navigator.navigateTo(Settings) },
            )

            GroupStrip(groups = groups, activeId = activeId)

            if (groups.isNotEmpty()) {
                SinglePaneGroups(
                    groups = groups,
                    activeId = activeId,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                )
            } else {
                EmptyEditorArea(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                )
            }

            FlowRow(
                modifier = Modifier
                    .background(Theme[colors][surface])
                    .height(IntrinsicSize.Max)
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var isCmd by remember { mutableStateOf(false) }

                KxButton(
                    onClick = { isCmd = !isCmd },
                    variant = if (isCmd) KxButtonVariant.Filled else KxButtonVariant.Outlined,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Cmd")
                }

                KxButton(
                    onClick = { action(InsertText("    ")) },
                    variant = KxButtonVariant.Outlined,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Tab")
                }
            }
        }
    }

    OpenProjectDialog(visible = openProjectDialog, onDismiss = { openProjectDialog = false })
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun ColumnScope.TopBar(
    onFilesClick: () -> Unit,
    onOpenDialog: () -> Unit,
    onSettingsClick: () -> Unit = {},
) {
    val editors: EditorStore = koinInject()
    val workspace: Workspace = koinInject()
    val hasVisibleFiles = rememberHasVisibleFiles()
    val showUndoRedo by workspace.showUndoRedoButtons.collectAsState()
    val canUndo by editors.canUndo.collectAsState()
    val canRedo by editors.canRedo.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KxButton(
            onClick = onFilesClick,
            variant = KxButtonVariant.Ghost,
            indication = null,
            contentPadding = PaddingValues(0.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$ ", style = Theme[typography][headline], color = Theme[colors][textMuted])
                Text("klyx", style = Theme[typography][headline])
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (hasVisibleFiles && showUndoRedo) {
            KxButton(
                onClick = { editors.undo() },
                enabled = canUndo,
                variant = KxButtonVariant.Ghost,
                bracketed = true,
                indication = null,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text("undo", style = Theme[typography][label])
            }

            KxButton(
                onClick = { editors.redo() },
                enabled = canRedo,
                variant = KxButtonVariant.Ghost,
                indication = null,
                bracketed = true,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            ) {
                Text("redo", style = Theme[typography][label])
            }

            Spacer(modifier = Modifier.width(6.dp))
        }

        TopBarOverflowMenu(onSettingsClick = onSettingsClick, showSplitAction = hasVisibleFiles)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProjectSwitcher(onOpenDialog = onOpenDialog)
    }
}

@Composable
private fun TopBarOverflowMenu(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    showSplitAction: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }

    UnstyledDropdownMenu(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        sideOffset = 4.dp,
        modifier = modifier,
        panel = {
            KxMenuPanel {
                if (showSplitAction) {
                    KxMenuItem(label = "split editor", onClick = { action(SplitEditor) })
                }
                KxMenuItem(label = "settings", onClick = onSettingsClick)
            }
        },
        anchor = {
            KxButton(
                onClick = { expanded = true },
                variant = KxButtonVariant.Outlined,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.defaultMinSize(minHeight = KxButtonDefaultMinHeight)
            ) {
                Text(text = "options", style = Theme[typography][label], color = Theme[colors][textSecondary])
            }
        },
    )
}

@Composable
private fun SinglePane(group: EditorGroup, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TabStrip(group = group, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(2.dp))

        EditorPane(
            group = group, modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp)
        )
    }
}

@Composable
private fun SinglePaneGroups(
    groups: List<EditorGroup>,
    activeId: EditorGroupId?,
    modifier: Modifier = Modifier,
) {
    val visibleId = activeId?.takeIf { id ->
        groups.any { group -> group.id == id }
    } ?: groups.firstOrNull()?.id

    Layout(
        modifier = modifier,
        content = {
            groups.forEach { group ->
                key(group.id) {
                    SinglePane(
                        group = group,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable -> measurable.measure(constraints) }
        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                if (groups[index].id == visibleId) {
                    placeable.place(0, 0)
                }
            }
        }
    }
}

@Composable
private fun EditorPane(group: EditorGroup?, modifier: Modifier = Modifier) {
    val workspace: Workspace = koinInject()

    val documents by workspace.documents.collectAsState()
    val activeProjectId by workspace.activeProjectId.collectAsState()
    val activeEditorGroupId by workspace.activeEditorGroupId.collectAsState()
    val tabScope by workspace.tabScope.collectAsState()

    if (group == null) {
        EmptyEditorArea(modifier)
        return
    }

    val focused = group.id == activeEditorGroupId
    val openedDocuments = group.documents.mapNotNull { documents[it.documentId] }
    val visibleDocuments = when (tabScope) {
        EditorTabScope.AllProjects -> openedDocuments
        EditorTabScope.CurrentProject -> openedDocuments.filter { it.isVisibleIn(activeProjectId) }
    }
    val activeDocumentId = group.activeDocumentId
        ?.takeIf { id -> visibleDocuments.any { it.id == id } }
        ?: visibleDocuments.firstOrNull()?.id

    if (openedDocuments.isEmpty()) {
        EmptyEditorArea(modifier)
        return
    }

    Box(modifier = modifier) {
        EditorStack(
            activeDocumentId = activeDocumentId,
            documents = openedDocuments,
            modifier = Modifier.fillMaxSize(),
        ) { document, active ->
            EditorDocument(
                document = document,
                active = active,
                focused = focused,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (activeDocumentId == null) {
            EmptyEditorArea(
                title = "no files open in this project",
                hint = "switch project or show all tabs in settings",
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun EditorDocument(
    document: Document,
    active: Boolean,
    focused: Boolean,
    modifier: Modifier = Modifier,
) {
    val workspace: Workspace = koinInject()
    val editors: EditorStore = koinInject()
    val controller = retain(document) { editors[document.id] }
    val focusRequester = remember { FocusRequester() }
    val settings = remember(active) { EditorSettings(readOnly = !active) }
    val theme = editorTheme()

    if (active && focused) {
        DisposableEffect(controller, document.id) {
            editors.setActive(document.id, controller)
            onDispose {
                editors.clearActive(controller)
            }
        }
    }

    LaunchedEffect(document.id) {
        controller
            .getDocument()
            .text
            .ifEmpty { workspace.readText(document.id) }
            .let { text ->
                controller.whenReady {
                    controller.loadDocument(text = text)
                }
            }
    }

    DisposableEffect(controller, document.id) {
        val unsubscribe = controller.onTextChanged { (kind, source) ->
            editors.refreshUndoState()
            if (source == EditorActionSource.KEYBOARD ||
                source == EditorActionSource.IME ||
                source == EditorActionSource.GESTURE ||
                kind == TextChangeKind.UNDO ||
                kind == TextChangeKind.REDO
            ) {
                action(PromotePreviewDocument(document.id))
            }
        }
        onDispose(unsubscribe)
    }

    LaunchedEffect(active, focused) {
        if (!active || !focused) return@LaunchedEffect
        withFrameNanos {}
        val _ = runCatching { focusRequester.requestFocus() }
    }

    SweetEditor(
        controller = controller,
        modifier = modifier.focusRequester(focusRequester),
        theme = theme,
        settings = settings,
    )
}

@Composable
private fun editorTheme(): EditorTheme = EditorTheme(
    backgroundColor = Theme[colors][background].toArgb(),
    textColor = Theme[colors][textPrimary].toArgb(),
    cursorColor = Theme[colors][accent].toArgb(),
    currentLineColor = Theme[colors][surface].copy(alpha = 0.2f).toArgb(),
    lineNumberColor = Theme[colors][textMuted].toArgb(),
    currentLineNumberColor = Theme[colors][accent].toArgb(),
    splitLineColor = Theme[colors][border].toArgb(),
    scrollbarTrackColor = Theme[colors][surface].copy(alpha = 0.3f).toArgb(),
    scrollbarThumbColor = Theme[colors][accent].copy(alpha = 0.5f).toArgb(),
    scrollbarThumbActiveColor = Theme[colors][accent].copy(alpha = 0.7f).toArgb(),
    selectionColor = Theme[colors][com.klyx.ui.accentSecondary].copy(alpha = 0.3f).toArgb(),
    selectionTextColor = Theme[colors][textPrimary].toArgb(),
    fontFamily = currentFontFamily(),
)

@Composable
private fun EditorStack(
    activeDocumentId: DocumentId?,
    documents: List<Document>,
    modifier: Modifier = Modifier,
    content: @Composable (Document, Boolean) -> Unit,
) {
    Layout(
        modifier = modifier,
        content = {
            documents.forEach { document ->
                key(document.id) {
                    content(document, activeDocumentId == document.id)
                }
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        layout(width = constraints.maxWidth, height = constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val document = documents[index]
                if (document.id == activeDocumentId) {
                    placeable.place(0, 0)
                }
            }
        }
    }
}

@Composable
private fun EmptyEditorArea(
    modifier: Modifier = Modifier,
    title: String = "no document open",
    hint: String = "open a file from files",
) {
    Box(modifier = modifier.background(Theme[colors][background]), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = Theme[typography][label], color = Theme[colors][textMuted])
            Text(
                text = hint,
                style = Theme[typography][labelSmall],
                color = Theme[colors][textMuted].copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun FileTreeDrawer(
    modifier: Modifier = Modifier,
    drawerState: DrawerState,
    onOpenDialog: () -> Unit,
    onNavigate: () -> Unit,
    content: @Composable () -> Unit,
) {
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp),
                drawerState = drawerState,
                drawerContainerColor = Theme[colors][background],
                modifier = Modifier
                    .width(platformFileTreeWidth)
                    .fillMaxHeight()
                    .imePadding(),
            ) {
                ExplorerPanel(onOpenDialog = onOpenDialog, onNavigate = onNavigate, modifier = Modifier.fillMaxSize())
            }
        },
        content = content,
    )
}

@Composable
private fun ExplorerPanel(
    onOpenDialog: () -> Unit,
    onNavigate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fs = FileSystem.SYSTEM
    val ids = remember { NodeIdRegistry() }
    val childrenLoader = remember(fs, ids) { fileTreeChildrenLoader(fs, ids) }
    val icons = rememberFileTreeIcons()
    val workspace: Workspace = koinInject()
    val treeScope = rememberCoroutineScope()

    val allProjects by workspace.projects.collectAsState()
    val activeProjectId by workspace.activeProjectId.collectAsState()
    val activeProject = activeProjectId?.let { allProjects[it] }

    val treeStates = remember { mutableMapOf<ProjectId, FileTreeState>() }
    LaunchedEffect(allProjects) {
        treeStates.keys.retainAll(allProjects.keys)
    }
    val state = remember(activeProjectId, treeScope, childrenLoader) {
        activeProjectId?.let { id -> treeStates.getOrPut(id) { FileTreeState(treeScope, childrenLoader) } }
    }
    val treeListState = remember(activeProjectId) { LazyListState() }

    LaunchedEffect(state, activeProject) {
        val tree = state ?: return@LaunchedEffect
        (val id, val projectRoot = root, val name) = activeProject ?: return@LaunchedEffect
        val rootId = ids.idFor("project", id.value)
        tree.visibleNodes.firstOrNull()?.let { existing ->
            if (existing.node.absolutePath == projectRoot.toString()) return@LaunchedEffect
            tree.removeNode(existing.node.id)
        }
        val root = withContext(IoDispatcher) {
            FileNode(
                id = rootId,
                name = name,
                absolutePath = projectRoot.toString(),
                isDirectory = true,
                childCount = runCatching { fs.listOrNull(projectRoot)?.size }.getOrNull(),
                projectId = id.value,
            )
        }
        tree.toggle(tree.addRoot(root))
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (allProjects.isEmpty()) {
            DrawerEmptyState(
                title = "No project open",
                subtitle = "Open a folder to browse files",
                actionLabel = "+ open project",
                onAction = onOpenDialog,
            )
            return@Column
        }
        if (activeProject == null) {
            DrawerEmptyState(
                title = "No project selected",
                subtitle = "Pick one from project selector",
                actionLabel = null,
                onAction = null,
            )
            return@Column
        }

        val tree = state ?: return@Column

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "FILES - ${activeProject.name}",
                style = Theme[typography][labelSmall],
                color = Theme[colors][textMuted],
                maxLines = 1,
            )
        }

        fun isOpen(node: FileNode): Boolean {
            val document = workspace.find(locationOf(node, workspace))
            return document != null && workspace.isDocumentOpen(document.id)
        }

        fun openFile(node: FileNode, preview: Boolean) {
            action(OpenDocument(location = locationOf(node, workspace), name = node.name, preview = preview))
            if (!node.isDirectory) onNavigate()
        }

        if (tree.visibleNodes.isEmpty()) {
            DrawerEmptyState(
                title = "Empty folder",
                subtitle = activeProject.root.toString(),
                actionLabel = null,
                onAction = null,
                compact = true,
            )
        } else {
            FileTree(
                state = tree,
                listState = treeListState,
                iconResolver = icons.resolver,
                contentSniffer = icons.sniffer,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                onFileClick = { visible ->
                    if (!visible.node.isDirectory) openFile(visible.node, preview = true)
                },
                onDoubleClick = { visible ->
                    if (!visible.node.isDirectory) openFile(visible.node, preview = false)
                },
                menuItems = { visible ->
                    if (visible.node.isDirectory) {
                        buildList {
                            add(FileTreeMenuItem("refresh") { tree.refresh(visible.node) })
                        }
                    } else {
                        val open = isOpen(visible.node)
                        buildList {
                            if (!open) {
                                add(FileTreeMenuItem("open") { openFile(visible.node, preview = false) })
                                add(FileTreeMenuItem("open preview") { openFile(visible.node, preview = true) })
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun DrawerEmptyState(
    title: String,
    subtitle: String,
    actionLabel: String?,
    onAction: (() -> Unit)?,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (compact) 12.dp else 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = Theme[typography][label], color = Theme[colors][textPrimary])
        Text(text = subtitle, style = Theme[typography][labelSmall], color = Theme[colors][textMuted], maxLines = 2)

        if (actionLabel != null && onAction != null) {
            KxButton(
                onClick = onAction,
                variant = KxButtonVariant.Outlined,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.defaultMinSize(minHeight = KxButtonDefaultMinHeight),
            ) {
                Text(text = actionLabel, style = Theme[typography][label])
            }
        }
    }
}

private fun locationOf(node: FileNode, workspace: Workspace): DocumentLocation {
    val projectId = node.projectId
    val path = node.absolutePath.toPath()
    return if (projectId == null) {
        DocumentLocation.Local(path)
    } else {
        val root = workspace[ProjectId(projectId)].root
        DocumentLocation.Project(ProjectId(projectId), path.relativeTo(root))
    }
}

private fun fileTreeChildrenLoader(
    fs: FileSystem,
    ids: NodeIdRegistry
): ChildrenLoader = { (id, absolutePath, projectId) ->
    withContext(IoDispatcher) {
        fs.listOrNull(absolutePath.toPath())?.map { path ->
            val meta = fs.metadata(path)
            val childCount = if (meta.isDirectory) fs.listOrNull(path)?.size else null
            FileNode(
                id = ids.idFor(id, path.toString()),
                name = path.name,
                absolutePath = path.toString(),
                isDirectory = meta.isDirectory,
                childCount = childCount,
                projectId = projectId,
            )
        }.orEmpty().sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }
}
