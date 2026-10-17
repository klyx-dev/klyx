package com.klyx.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.RectangleShape
import com.composeunstyled.Text
import com.composeunstyled.UnstyledDropdownMenu
import com.composeunstyled.theme.Theme
import com.klyx.ui.LocalCornerRadius
import com.klyx.ui.kxShape
import com.klyx.ui.kxSmallShape
import com.klyx.core.Debug
import com.klyx.core.IoDispatcher
import com.klyx.core.action.action
import com.klyx.editor.EditorStore
import com.klyx.lsp.CodeAction
import com.klyx.lsp.Diagnostic
import com.klyx.lsp.DiagnosticSeverity
import com.klyx.lsp.EditorLsp
import com.klyx.lsp.FormatDocument
import com.klyx.lsp.LspDocumentUtils
import com.klyx.lsp.LspSessionState
import com.klyx.lsp.RestartLanguageServer
import com.klyx.navigation.LocalNavigator
import com.klyx.navigation.Settings
import com.klyx.runtime.action.CloseDocument
import com.klyx.runtime.action.Copy
import com.klyx.runtime.action.FoldAll
import com.klyx.runtime.action.InsertText
import com.klyx.runtime.action.OpenDocument
import com.klyx.runtime.action.OpenProject
import com.klyx.runtime.action.Paste
import com.klyx.runtime.action.PromotePreviewDocument
import com.klyx.runtime.action.SaveActiveDocument
import com.klyx.runtime.action.SaveAllDocuments
import com.klyx.runtime.action.SelectAll
import com.klyx.runtime.action.SplitEditor
import com.klyx.runtime.action.UnfoldAll
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
import com.klyx.ui.accentSecondary
import com.klyx.ui.background
import com.klyx.ui.bodySmall
import com.klyx.ui.border
import com.klyx.ui.colors
import com.klyx.ui.components.KxButton
import com.klyx.ui.components.KxButtonDefaultMinHeight
import com.klyx.ui.components.KxButtonVariant
import com.klyx.ui.components.KxDialog
import com.klyx.ui.components.KxMenuItem
import com.klyx.ui.components.KxMenuPanel
import com.klyx.ui.components.KxMenuSeparator
import com.klyx.ui.components.KxTextField
import com.klyx.ui.currentFontFamily
import com.klyx.ui.editor.CodeActionsDialog
import com.klyx.ui.editor.CompletionDocDialog
import com.klyx.ui.editor.CustomCompletionPopup
import com.klyx.ui.editor.DiagnosticBanner
import com.klyx.ui.editor.GroupStrip
import com.klyx.ui.editor.HoverCard
import com.klyx.ui.editor.LspCompletionItemUi
import com.klyx.ui.editor.LspStatusDialog
import com.klyx.ui.editor.ProblemsDialog
import com.klyx.ui.editor.TabStrip
import com.klyx.ui.editor.rememberEditorGroups
import com.klyx.ui.editor.rememberHasVisibleFiles
import com.klyx.ui.error
import com.klyx.ui.filetree.ChildrenLoader
import com.klyx.ui.filetree.FileNode
import com.klyx.ui.filetree.FileTree
import com.klyx.ui.filetree.FileTreeMenuItem
import com.klyx.ui.filetree.FileTreeMenuSeparator
import com.klyx.ui.filetree.FileTreeState
import com.klyx.ui.filetree.NodeIdRegistry
import com.klyx.ui.filetree.VisibleFileNode
import com.klyx.ui.filetree.platformFileTreeWidth
import com.klyx.ui.filetree.rememberFileTreeIcons
import com.klyx.ui.headline
import com.klyx.ui.label
import com.klyx.ui.labelSmall
import com.klyx.ui.project.OpenProjectDialog
import com.klyx.ui.project.ProjectSwitcher
import com.klyx.ui.surface
import com.klyx.ui.surfaceVariant
import com.klyx.ui.textMuted
import com.klyx.ui.textPrimary
import com.klyx.ui.textSecondary
import com.klyx.ui.typography
import com.klyx.ui.warning
import io.github.lumkit.sweeteditor.EditorActionSource
import io.github.lumkit.sweeteditor.EditorSettings
import io.github.lumkit.sweeteditor.EditorTheme
import io.github.lumkit.sweeteditor.SweetEditor
import io.github.lumkit.sweeteditor.TextChangeKind
import io.github.lumkit.sweeteditor.highlight.HighlightDocumentDescriptor
import io.github.lumkit.sweeteditor.highlight.HighlightFeatureFlags
import io.github.lumkit.sweeteditor.highlight.HighlightTheme
import io.github.lumkit.sweeteditor.highlight.SweetLineHighlight
import io.github.lumkit.sweeteditor.highlight.SweetLineHighlightConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.SYSTEM
import org.koin.compose.koinInject
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var openProjectDialog by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.current

    val workspace: Workspace = koinInject()
    val editors: EditorStore = koinInject()
    val lsp: EditorLsp = koinInject()

    val documents by workspace.documents.collectAsState()
    val activeDocId by editors.activeDocumentId.collectAsState()
    val activeDoc = documents[activeDocId]

    var showProblemsDialog by remember { mutableStateOf(false) }
    var showLspStatusDialog by remember { mutableStateOf(false) }

    var showCodeActionsDialog by remember { mutableStateOf(false) }
    var availableCodeActions by remember { mutableStateOf<List<CodeAction>>(emptyList()) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2.5.seconds)
            toastMessage = null
        }
    }

    LaunchedEffect(lsp) {
        lsp.lspErrors.collect { message ->
            toastMessage = message
        }
    }

    val allDiags by lsp.client.diagnostics.collectAsState()
    val activeDocDiags = remember(allDiags, activeDoc?.id) {
        val doc = activeDoc ?: return@remember emptyList()
        val uri = LspDocumentUtils.uriOf(doc, workspace)
        val normalized = lsp.client.normalizeUri(uri)
        allDiags.entries.firstOrNull { lsp.client.normalizeUri(it.key) == normalized }?.value ?: emptyList()
    }

    val onQuickFix: (Diagnostic) -> Unit = { diag ->
        if (activeDoc != null) {
            scope.launch {
                val actions = lsp.getCodeActions(activeDoc, diag.range, listOf(diag))
                if (actions.isNotEmpty()) {
                    availableCodeActions = actions
                    showCodeActionsDialog = true
                } else {
                    toastMessage = "No quick fixes available for this problem"
                }
            }
        }
    }

    Debug {
        try {
            action(OpenProject(systemHomeDirectory / "zed/Projects/rs-test"))
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            toastMessage = "Failed to open project: ${e.message}"
        }
    }

    val activeProjectId by workspace.activeProjectId.collectAsState()
    val tabScope by workspace.tabScope.collectAsState()

    (val groups = first, val activeId = second) = rememberEditorGroups()

    LaunchedEffect(activeProjectId, tabScope) {
        if (tabScope != EditorTabScope.CurrentProject) return@LaunchedEffect
        val docs = workspace.documents.value
        workspace.editorGroups.value.forEach { (id, documents, activeDocumentId) ->
            val visible = documents.filter { tab ->
                docs[tab.documentId]?.isVisibleIn(activeProjectId) ?: true
            }
            val selectionVisible = activeDocumentId?.let { active ->
                visible.any { it.documentId == active }
            } == true
            if (!selectionVisible && visible.isNotEmpty()) {
                workspace.activateDocument(id, visible.first().documentId)
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
                onShowProblems = { showProblemsDialog = true },
                onShowLspStatus = { showLspStatusDialog = true },
            )

            GroupStrip(groups = groups, activeId = activeId)

            if (groups.isNotEmpty()) {
                SinglePaneGroups(
                    groups = groups,
                    activeId = activeId,
                    onQuickFix = onQuickFix,
                    onShowToast = { msg -> toastMessage = msg },
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
                    onClick = { action(InsertText("\t")) },
                    variant = KxButtonVariant.Outlined,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text("Tab")
                }

                if (isCmd) {
                    KxButton(
                        onClick = { action(SelectAll) },
                        variant = KxButtonVariant.Outlined,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Select All")
                    }

                    KxButton(
                        onClick = { action(Copy) },
                        variant = KxButtonVariant.Outlined,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Copy")
                    }

                    KxButton(
                        onClick = { action(Paste) },
                        variant = KxButtonVariant.Outlined,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Paste")
                    }

                    KxButton(
                        onClick = { action(FoldAll) },
                        variant = KxButtonVariant.Outlined,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Fold")
                    }

                    KxButton(
                        onClick = { action(UnfoldAll) },
                        variant = KxButtonVariant.Outlined,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text("Unfold")
                    }
                }
            }

            if (activeDoc != null) {
                StatusBar(
                    activeDoc = activeDoc,
                    activeDiags = activeDocDiags,
                    onShowProblems = { showProblemsDialog = true },
                    onShowLspStatus = { showLspStatusDialog = true },
                )
            }
        }
    }

    AnimatedVisibility(
        visible = toastMessage != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
        modifier = Modifier
            .fillMaxWidth()
            .safeDrawingPadding()
            .padding(top = 16.dp),
    ) {
        toastMessage?.let { msg ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter,
            ) {
                val toastShape = kxShape()
                Box(
                    modifier = Modifier
                        .shadow(8.dp, toastShape)
                        .clip(toastShape)
                        .background(Theme[colors][surfaceVariant], toastShape)
                        .border(1.dp, Theme[colors][border], toastShape)
                        .clickable { toastMessage = null }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "ℹ",
                            style = Theme[typography][bodySmall].copy(fontWeight = FontWeight.Bold),
                            color = Theme[colors][accentSecondary],
                        )
                        Text(
                            text = msg,
                            style = Theme[typography][bodySmall],
                            color = Theme[colors][textPrimary],
                        )
                    }
                }
            }
        }
    }

    OpenProjectDialog(visible = openProjectDialog, onDismiss = { openProjectDialog = false })

    if (activeDoc != null) {
        ProblemsDialog(
            visible = showProblemsDialog,
            onDismiss = { showProblemsDialog = false },
            documentName = activeDoc.name,
            diagnostics = activeDocDiags,
            onSelectDiagnostic = { diag ->
                val ctrl = editors[activeDoc.id]
                ctrl.setCursorPosition(diag.range.start.line.toInt(), diag.range.start.character.toInt())
                ctrl.ensureCursorVisible()
            },
            onQuickFix = onQuickFix,
        )
    }

    CodeActionsDialog(
        visible = showCodeActionsDialog,
        onDismiss = { showCodeActionsDialog = false },
        actions = availableCodeActions,
        onSelectAction = { action ->
            scope.launch {
                lsp.applyCodeAction(action)
            }
        },
    )

    val activeSessions by lsp.activeSessions.collectAsState()
    val progressItems by lsp.client.progressItems.collectAsState()
    LspStatusDialog(
        visible = showLspStatusDialog,
        onDismiss = { showLspStatusDialog = false },
        sessions = activeSessions,
        progressItems = progressItems,
        onRestartLanguageServer = { lang ->
            scope.launch {
                val _ = lsp.restartSession(lang)
            }
        },
    )
}

private enum class OverflowMenuPage {
    Main,
    Lsp,
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun ColumnScope.TopBar(
    onFilesClick: () -> Unit,
    onOpenDialog: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onShowProblems: () -> Unit = {},
    onShowLspStatus: () -> Unit = {},
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

        TopBarOverflowMenu(
            onSettingsClick = onSettingsClick,
            onShowProblems = onShowProblems,
            onShowLspStatus = onShowLspStatus,
            showSplitAction = hasVisibleFiles,
        )
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
    onShowProblems: () -> Unit,
    onShowLspStatus: () -> Unit,
    modifier: Modifier = Modifier,
    showSplitAction: Boolean = true,
) {
    val workspace: Workspace = koinInject()
    val editors: EditorStore = koinInject()
    val lsp: EditorLsp = koinInject()
    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }
    var menuPage by remember { mutableStateOf(OverflowMenuPage.Main) }

    val activeDocId by editors.activeDocumentId.collectAsState()
    val hasActiveDoc = activeDocId != null

    UnstyledDropdownMenu(
        expanded = expanded,
        onExpandedChange = {
            expanded = it
            if (!it) menuPage = OverflowMenuPage.Main
        },
        sideOffset = 4.dp,
        modifier = modifier,
        panel = {
            KxMenuPanel {
                when (menuPage) {
                    OverflowMenuPage.Main -> {
                        KxMenuItem(
                            label = "save",
                            trailing = "Ctrl+S",
                            enabled = hasActiveDoc,
                            onClick = {
                                action(SaveActiveDocument)
                                expanded = false
                            },
                        )
                        KxMenuItem(
                            label = "save all",
                            trailing = "Ctrl+Shift+S",
                            onClick = {
                                action(SaveAllDocuments)
                                expanded = false
                            },
                        )
                        KxMenuItem(
                            label = "format document",
                            enabled = hasActiveDoc,
                            onClick = {
                                action(FormatDocument)
                                expanded = false
                            },
                        )
                        KxMenuItem(
                            label = "LSP & tools",
                            trailing = "▶",
                            closeOnClick = false,
                            onClick = {
                                menuPage = OverflowMenuPage.Lsp
                            },
                        )
                        KxMenuSeparator()
                        if (showSplitAction) {
                            KxMenuItem(
                                label = "split editor",
                                onClick = {
                                    action(SplitEditor)
                                    expanded = false
                                },
                            )
                        }
                        KxMenuItem(
                            label = "settings",
                            onClick = {
                                onSettingsClick()
                                expanded = false
                            },
                        )
                    }

                    OverflowMenuPage.Lsp -> {
                        KxMenuItem(
                            label = "◀ back",
                            closeOnClick = false,
                            onClick = { menuPage = OverflowMenuPage.Main },
                        )
                        KxMenuSeparator()
                        KxMenuItem(
                            label = "restart server",
                            enabled = hasActiveDoc,
                            onClick = {
                                action(RestartLanguageServer)
                                expanded = false
                            },
                        )
                        KxMenuItem(
                            label = "view problems",
                            onClick = {
                                onShowProblems()
                                expanded = false
                            },
                        )
                        KxMenuItem(
                            label = "server status & tasks",
                            onClick = {
                                onShowLspStatus()
                                expanded = false
                            },
                        )
                        KxMenuItem(
                            label = "hover documentation",
                            enabled = hasActiveDoc,
                            onClick = {
                                expanded = false
                                scope.launch {
                                    val doc = activeDocId?.let { workspace.document(it) } ?: return@launch
                                    (val line, val column) = editors.activeCursor.value ?: return@launch
                                    val _ = lsp.requestHover(doc, line, column)
                                }
                            },
                        )
                    }
                }
            }
        },
        anchor = {
            KxButton(
                onClick = { expanded = true },
                variant = KxButtonVariant.Outlined,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.defaultMinSize(minHeight = KxButtonDefaultMinHeight),
            ) {
                Text(text = "options", style = Theme[typography][label], color = Theme[colors][textSecondary])
            }
        },
    )
}

@Composable
private fun SinglePane(
    group: EditorGroup,
    onQuickFix: (Diagnostic) -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TabStrip(group = group, modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(2.dp))

        EditorPane(
            group = group,
            onQuickFix = onQuickFix,
            onShowToast = onShowToast,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 2.dp),
        )
    }
}

@Composable
private fun SinglePaneGroups(
    groups: List<EditorGroup>,
    activeId: EditorGroupId?,
    onQuickFix: (Diagnostic) -> Unit,
    onShowToast: (String) -> Unit,
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
                        onQuickFix = onQuickFix,
                        onShowToast = onShowToast,
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
private fun EditorPane(
    group: EditorGroup?,
    onQuickFix: (Diagnostic) -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                onQuickFix = onQuickFix,
                onShowToast = onShowToast,
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
    onQuickFix: (Diagnostic) -> Unit,
    onShowToast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val workspace: Workspace = koinInject()
    val editors: EditorStore = koinInject()
    val controller = retain(document) { editors[document.id] }
    val focusRequester = remember { FocusRequester() }
    val settings = remember(active) { EditorSettings(readOnly = !active) }
    val theme = editorTheme()

    val highlight = remember(document) {
        SweetLineHighlight(
            SweetLineHighlightConfig(
                document = HighlightDocumentDescriptor(document.name),
                features = HighlightFeatureFlags(
                    syntaxHighlight = true,
                    indentGuides = false,
                    bracketGuides = true,
                    matchedBrackets = true,
                    rainbowBrackets = true,
                ),
                theme = HighlightTheme.dark()
            )
        )
    }

    DisposableEffect(controller, highlight) {
        val binding = highlight.bind(controller)
        onDispose(binding::close)
    }

    val lsp: EditorLsp = koinInject()
    val scope = rememberCoroutineScope()

    DisposableEffect(controller, document.id) {
        editors.register(document.id, controller)
        lsp.bindEditor(document, controller)
        onDispose {
            lsp.unbindEditor(document)
            editors.unregister(document.id, controller)
        }
    }

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
                    scope.launch {
                        val uri = LspDocumentUtils.uriOf(document, workspace)
                        lsp.client.applyDiagnosticsToController(uri, controller)
                    }
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

    val allDiags by lsp.client.diagnostics.collectAsState()
    val fileDiags = remember(allDiags, document.id) {
        val uri = LspDocumentUtils.uriOf(document, workspace)
        val normalized = lsp.client.normalizeUri(uri)
        allDiags.entries.firstOrNull { lsp.client.normalizeUri(it.key) == normalized }?.value ?: emptyList()
    }

    LaunchedEffect(fileDiags, controller) {
        controller.whenReady {
            scope.launch {
                val uri = LspDocumentUtils.uriOf(document, workspace)
                lsp.client.applyDiagnosticsToController(uri, controller)
            }
        }
    }

    var cursorLine by remember { mutableStateOf<Int?>(null) }
    var dismissedLine by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(fileDiags) {
        dismissedLine = null
    }

    DisposableEffect(controller, active, focused) {
        val unsubscribe = controller.onCursorChanged { event ->
            cursorLine = event.cursorPosition.line
            if (active && focused) {
                editors.updateCursor(event.cursorPosition.line, event.cursorPosition.column)
            }
        }
        onDispose(unsubscribe)
    }

    LaunchedEffect(controller, active, focused) {
        controller.whenReady {
            cursorLine = controller.getCursorPosition()?.line
            val pos = controller.getCursorPosition()
            if (pos != null && active && focused) {
                editors.updateCursor(pos.line, pos.column)
            }
        }
    }

    val activeDiags = remember(fileDiags, cursorLine, dismissedLine) {
        val line = cursorLine ?: return@remember emptyList()
        if (line == dismissedLine) return@remember emptyList()
        fileDiags.filter { diag ->
            val startLine = diag.range.start.line.toInt()
            val endLine = diag.range.end.line.toInt()
            line in startLine..endLine
        }
    }

    LaunchedEffect(active, focused) {
        if (!active || !focused) return@LaunchedEffect
        withFrameNanos {}
        val _ = runCatching { focusRequester.requestFocus() }
    }

    var editorBoxHeightPx by remember { mutableStateOf(0) }
    var hoverScrollTick by remember { mutableStateOf(0) }

    DisposableEffect(controller) {
        val unsubscribe = controller.onScrollChanged { hoverScrollTick++ }
        onDispose(unsubscribe)
    }

    val activeHover by lsp.activeHover.collectAsState()
    val hoverRect = remember(activeHover, hoverScrollTick) {
        (val line, val column) = activeHover ?: return@remember null
        runCatching { controller.getPositionRect(line, column) }.getOrNull()
    }

    val density = LocalDensity.current

    val hoverYOffsetPx = remember(hoverRect, editorBoxHeightPx) {
        val marginPx = with(density) { 8.dp.roundToPx() }
        if (hoverRect == null || editorBoxHeightPx <= 0) {
            marginPx
        } else {
            val below = (hoverRect.y + hoverRect.height).roundToInt() + marginPx
            val estCardPx = with(density) { 280.dp.roundToPx() }
            if (below + estCardPx <= editorBoxHeightPx) {
                below
            } else {
                (hoverRect.y.roundToInt() - marginPx - estCardPx).coerceAtLeast(marginPx)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { editorBoxHeightPx = it.height },
    ) {
        SweetEditor(
            controller = controller,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester),
            theme = theme,
            settings = settings,
        )

        if (active && activeHover != null) {
            HoverCard(
                hover = activeHover!!,
                onDismiss = { lsp.dismissHover() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .offset { IntOffset(0, hoverYOffsetPx) },
            )
        }

        val activeCompletion by lsp.activeCompletion.collectAsState()
        var docItemToShow by remember { mutableStateOf<LspCompletionItemUi?>(null) }

        if (active && activeCompletion != null && activeCompletion?.documentId == document.id) {
            CustomCompletionPopup(
                state = activeCompletion!!,
                onSelectItem = { },
                onConfirmItem = { item ->
                    lsp.applyCompletion(item, controller)
                },
                onShowDoc = { item ->
                    scope.launch {
                        val resolved = lsp.resolveCompletionDoc(document, item)
                        val doc = resolved.documentation?.trim()
                        if (doc.isNullOrBlank()) {
                            onShowToast("No documentation available for \"${item.label}\"")
                        } else {
                            docItemToShow = resolved
                        }
                    }
                },
                onDismiss = {
                    lsp.dismissCompletionPopup()
                },
            )
        }

        if (docItemToShow != null) {
            CompletionDocDialog(
                item = docItemToShow!!,
                onDismiss = { docItemToShow = null },
            )
        }

        if (active && activeDiags.isNotEmpty()) {
            DiagnosticBanner(
                diagnostics = activeDiags,
                onDismiss = { dismissedLine = cursorLine },
                onQuickFix = onQuickFix,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun StatusBar(
    activeDoc: Document?,
    activeDiags: List<Diagnostic>,
    onShowProblems: () -> Unit,
    onShowLspStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lsp: EditorLsp = koinInject()
    val editors: EditorStore = koinInject()
    val activeSessions by lsp.activeSessions.collectAsState()
    val progressItems by lsp.client.progressItems.collectAsState()
    val cursor by editors.activeCursor.collectAsState()

    val langId = activeDoc?.let { LspDocumentUtils.languageIdOf(it) }
    val currentSessionState = langId?.let { activeSessions[it] }
    val pillShape = kxSmallShape()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Theme[colors][surfaceVariant])
            .border(width = 0.5.dp, color = Theme[colors][border])
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(pillShape)
                .clickable(onClick = onShowLspStatus)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dotColor = when (currentSessionState) {
                LspSessionState.Running -> Theme[colors][accent]
                LspSessionState.Starting -> Theme[colors][warning]
                LspSessionState.Error -> Theme[colors][error]
                LspSessionState.Stopped, null -> Theme[colors][textMuted]
            }
            val statusSymbol = when (currentSessionState) {
                LspSessionState.Running -> "●"
                LspSessionState.Starting -> "◐"
                LspSessionState.Error -> "●"
                LspSessionState.Stopped, null -> "○"
            }
            Text(
                text = statusSymbol,
                style = Theme[typography][labelSmall],
                color = dotColor,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = langId ?: "plain text",
                style = Theme[typography][labelSmall],
                color = Theme[colors][textSecondary],
            )
        }

        if (progressItems.isNotEmpty()) {
            Spacer(modifier = Modifier.width(8.dp))
            (val title, val percentage) = progressItems.values.first()
            val percentText = percentage?.let { " $it%" } ?: ""
            val extraCount = if (progressItems.size > 1) " (+${progressItems.size - 1})" else ""
            Row(
                modifier = Modifier
                    .clip(pillShape)
                    .background(Theme[colors][accent].copy(alpha = 0.12f), pillShape)
                    .clickable(onClick = onShowLspStatus)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "$title$percentText$extraCount",
                    style = Theme[typography][labelSmall],
                    color = Theme[colors][accent],
                    maxLines = 1,
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        if (activeDiags.isNotEmpty()) {
            val errorCount = activeDiags.count { it.severity == DiagnosticSeverity.Error || it.severity == null }
            val warnCount = activeDiags.count { it.severity == DiagnosticSeverity.Warning }

            Row(
                modifier = Modifier
                    .clip(pillShape)
                    .clickable(onClick = onShowProblems)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (errorCount > 0) {
                    Text(
                        text = "✕ $errorCount",
                        style = Theme[typography][labelSmall].copy(fontWeight = FontWeight.Bold),
                        color = Theme[colors][error],
                    )
                    if (warnCount > 0) Spacer(modifier = Modifier.width(6.dp))
                }
                if (warnCount > 0) {
                    Text(
                        text = "$warnCount",
                        style = Theme[typography][labelSmall].copy(fontWeight = FontWeight.Bold),
                        color = Theme[colors][warning],
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        if (cursor != null) {
            Text(
                text = "Ln ${cursor!!.line + 1}, Col ${cursor!!.column + 1}",
                style = Theme[typography][labelSmall],
                color = Theme[colors][textMuted],
            )
        }
    }
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
    selectionColor = Theme[colors][accentSecondary].copy(alpha = 0.3f).toArgb(),
    selectionTextColor = Theme[colors][textPrimary].toArgb(),
    diagnosticErrorColor = Theme[colors][error].toArgb(),
    diagnosticWarningColor = Theme[colors][warning].toArgb(),
    diagnosticInfoColor = Theme[colors][accentSecondary].toArgb(),
    diagnosticHintColor = Theme[colors][textMuted].toArgb(),
    codeLensColor = Theme[colors][textMuted].toArgb(),
    activeCodeLensColor = Theme[colors][accent].toArgb(),
    documentHighlightTextBgColor = Theme[colors][accent].copy(alpha = 0.18f).toArgb(),
    documentHighlightReadBgColor = Theme[colors][accent].copy(alpha = 0.18f).toArgb(),
    documentHighlightWriteBgColor = Theme[colors][accentSecondary].copy(alpha = 0.25f).toArgb(),
    compositionUnderlineColor = Theme[colors][accent].toArgb(),
    bracketHighlightBgColor = Theme[colors][accent].copy(alpha = 0.15f).toArgb(),
    bracketHighlightBorderColor = Theme[colors][accent].toArgb(),
    selectionMenuBgColor = Theme[colors][surface].toArgb(),
    selectionMenuTextColor = Theme[colors][textPrimary].toArgb(),
    selectionMenuDividerColor = Theme[colors][border].toArgb(),
    contextMenuBgColor = Theme[colors][surface].toArgb(),
    contextMenuTextColor = Theme[colors][textPrimary].toArgb(),
    contextMenuDividerColor = Theme[colors][border].toArgb(),
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
            val radius = LocalCornerRadius.current
            val drawerShape = if (radius > 0.dp) RoundedCornerShape(topEnd = radius, bottomEnd = radius) else RectangleShape
            ModalDrawerSheet(
                drawerShape = drawerShape,
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

    var createRequest by remember { mutableStateOf<Pair<FileNode, Boolean>?>(null) }
    var renameRequest by remember { mutableStateOf<FileNode?>(null) }
    var deleteRequest by remember { mutableStateOf<FileNode?>(null) }

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

        fun openFile(node: FileNode, preview: Boolean) {
            action(OpenDocument(location = locationOf(node, workspace), name = node.name, preview = preview))
            if (!node.isDirectory) onNavigate()
        }

        fun targetDirFor(node: FileNode): Path =
            if (node.isDirectory) node.absolutePath.toPath()
            else node.absolutePath.toPath().parent ?: activeProject.root

        fun findVisibleByPath(path: String): VisibleFileNode? =
            tree.visibleNodes.firstOrNull { it.node.absolutePath == path }

        fun refreshDir(dir: Path) {
            val vn = findVisibleByPath(dir.toString()) ?: return
            if (tree.isExpanded(vn.node.id)) tree.refresh(vn.node) else tree.toggle(vn)
        }

        fun closeDocumentsUnder(path: Path) {
            val raw = path.toString()
            val prefix = raw.removeSuffix("/") + "/"
            workspace.documents.value.values.toList().forEach { doc ->
                val p = runCatching { workspace.resolve(doc).toString() }.getOrNull() ?: return@forEach
                if (p == raw || p.startsWith(prefix)) {
                    val _ = runCatching { action(CloseDocument(doc.id)) }
                }
            }
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
                    val node = visible.node
                    val isRoot = node.absolutePath == activeProject.root.toString()
                    buildList {
                        if (node.isDirectory) {
                            add(FileTreeMenuItem("new file") { createRequest = node to false })
                            add(FileTreeMenuItem("new folder") { createRequest = node to true })
                            add(FileTreeMenuSeparator)
                            add(FileTreeMenuItem("refresh") { tree.refresh(node) })
                            if (!isRoot) {
                                add(FileTreeMenuItem("rename") { renameRequest = node })
                                add(FileTreeMenuItem("delete", danger = true) { deleteRequest = node })
                            }
                        } else {
                            val doc = runCatching { workspace.find(locationOf(node, workspace)) }.getOrNull()
                            val tab = doc?.let { d ->
                                workspace.editorGroups.value.flatMap { it.documents }
                                    .firstOrNull { it.documentId == d.id }
                            }
                            if (doc == null || tab == null) {
                                add(FileTreeMenuItem("open") { openFile(node, preview = false) })
                                add(FileTreeMenuItem("open preview") { openFile(node, preview = true) })
                            } else {
                                add(FileTreeMenuItem("open") { openFile(node, preview = false) })
                                if (tab.preview) {
                                    add(FileTreeMenuItem("keep open") { action(PromotePreviewDocument(doc.id)) })
                                }
                                add(FileTreeMenuItem("close") { action(CloseDocument(doc.id)) })
                            }
                            add(FileTreeMenuSeparator)
                            add(FileTreeMenuItem("rename") { renameRequest = node })
                            add(FileTreeMenuItem("delete", danger = true) { deleteRequest = node })
                        }
                    }
                },
            )
        }

        val createTarget = createRequest
        if (createTarget != null) {
            val target = createTarget.first
            val isFolder = createTarget.second
            FileNameDialog(
                visible = true,
                title = if (isFolder) "New folder" else "New file",
                subtitle = "In ${target.absolutePath}",
                initial = "",
                confirmLabel = "create",
                onDismiss = { createRequest = null },
                onConfirm = { name ->
                    val dir = targetDirFor(target)
                    val newPath = dir / name
                    try {
                        withContext(IoDispatcher) {
                            if (fs.exists(newPath)) error("Already exists")
                            if (isFolder) {
                                fs.createDirectories(newPath)
                            } else {
                                if (!fs.exists(dir)) fs.createDirectories(dir)
                                fs.write(newPath) { }
                            }
                        }
                        refreshDir(dir)
                        if (!isFolder) {
                            openFile(
                                FileNode(
                                    id = "",
                                    name = name,
                                    absolutePath = newPath.toString(),
                                    isDirectory = false,
                                    projectId = target.projectId,
                                ),
                                preview = true,
                            )
                        }
                        null
                    } catch (t: Throwable) {
                        if (t is CancellationException) {
                            currentCoroutineContext().ensureActive()
                        }
                        t.message ?: "Failed to create"
                    }
                },
            )
        }

        val renameTarget = renameRequest
        if (renameTarget != null) {
            FileNameDialog(
                visible = true,
                title = "Rename",
                subtitle = renameTarget.absolutePath,
                initial = renameTarget.name,
                confirmLabel = "rename",
                onDismiss = { renameRequest = null },
                onConfirm = { name ->
                    if (name == renameTarget.name) return@FileNameDialog null
                    val src = renameTarget.absolutePath.toPath()
                    val parent = src.parent ?: return@FileNameDialog "Cannot rename here"
                    val dst = parent / name
                    try {
                        withContext(Dispatchers.IO) {
                            if (fs.exists(dst)) error("Already exists")
                            fs.atomicMove(src, dst)
                        }
                        closeDocumentsUnder(src)
                        refreshDir(parent)
                        null
                    } catch (t: Throwable) {
                        if (t is CancellationException) {
                            currentCoroutineContext().ensureActive()
                        }
                        t.message ?: "Failed to rename"
                    }
                },
            )
        }

        val deleteTarget = deleteRequest
        if (deleteTarget != null) {
            ConfirmDeleteDialog(
                visible = true,
                name = deleteTarget.name,
                subtitle = deleteTarget.absolutePath,
                onDismiss = { deleteRequest = null },
                onConfirm = {
                    val path = deleteTarget.absolutePath.toPath()
                    val parent = path.parent
                    try {
                        withContext(IoDispatcher) {
                            fs.deleteRecursively(path)
                        }
                        closeDocumentsUnder(path)
                        if (parent != null) refreshDir(parent)
                        null
                    } catch (t: Throwable) {
                        if (t is CancellationException) {
                            currentCoroutineContext().ensureActive()
                        }
                        t.message ?: "Failed to delete"
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

@Composable
private fun FileNameDialog(
    visible: Boolean,
    title: String,
    subtitle: String?,
    initial: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: suspend (String) -> String?,
) {
    val field = rememberTextFieldState(initial)
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(visible, initial) {
        if (visible) {
            error = null
            busy = false
            field.edit { replace(0, length, initial) }
        }
    }

    KxDialog(
        visible = visible,
        onDismissRequest = { if (!busy) onDismiss() },
        title = title,
        subtitle = subtitle,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxButton(
                    onClick = { if (!busy) onDismiss() },
                    variant = KxButtonVariant.Ghost,
                    enabled = !busy,
                ) {
                    Text(text = "cancel", style = Theme[typography][label])
                }
                KxButton(
                    onClick = {
                        val name = field.text.toString().trim()
                        if (name.isEmpty()) {
                            error = "Name is empty"
                            return@KxButton
                        }
                        if (name.contains("/")) {
                            error = "Name cannot contain /"
                            return@KxButton
                        }
                        scope.launch {
                            busy = true
                            val err = onConfirm(name)
                            busy = false
                            if (err == null) onDismiss() else error = err
                        }
                    },
                    variant = KxButtonVariant.Filled,
                    enabled = !busy,
                ) {
                    Text(text = confirmLabel, style = Theme[typography][label])
                }
            }
        },
    ) {
        KxTextField(state = field, label = "Name", placeholder = "untitled.txt")
        if (error != null) {
            Text(
                text = error!!,
                style = Theme[typography][bodySmall],
                color = Theme[colors][com.klyx.ui.error],
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(
    visible: Boolean,
    name: String,
    subtitle: String?,
    onDismiss: () -> Unit,
    onConfirm: suspend () -> String?,
) {
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(visible) {
        if (visible) {
            error = null
            busy = false
        }
    }

    KxDialog(
        visible = visible,
        onDismissRequest = { if (!busy) onDismiss() },
        title = "Delete \"$name\"?",
        subtitle = subtitle,
        actions = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KxButton(
                    onClick = { if (!busy) onDismiss() },
                    variant = KxButtonVariant.Ghost,
                    enabled = !busy,
                ) {
                    Text(text = "cancel", style = Theme[typography][label])
                }
                KxButton(
                    onClick = {
                        scope.launch {
                            busy = true
                            val err = onConfirm()
                            busy = false
                            if (err == null) onDismiss() else error = err
                        }
                    },
                    backgroundColor = Theme[colors][com.klyx.ui.error],
                    variant = KxButtonVariant.Filled,
                    enabled = !busy,
                ) {
                    Text(text = "delete", style = Theme[typography][label])
                }
            }
        },
    ) {
        Text(
            text = "This cannot be undone.",
            style = Theme[typography][bodySmall],
            color = Theme[colors][textMuted],
        )
        if (error != null) {
            Text(
                text = error!!,
                style = Theme[typography][bodySmall],
                color = Theme[colors][com.klyx.ui.error],
                modifier = Modifier.padding(top = 6.dp),
            )
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
