package com.klyx.ui.palette

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.composeunstyled.Text
import com.composeunstyled.theme.Theme
import com.klyx.core.action.action
import com.klyx.editor.DocumentJump
import com.klyx.editor.EditorStore
import com.klyx.lsp.EditorLsp
import com.klyx.lsp.rememberCursor
import com.klyx.runtime.action.ActivateDocument
import com.klyx.runtime.action.OpenDocument
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.Workspace
import com.klyx.runtime.workspace.isVisibleIn
import com.klyx.ui.components.KxDialog
import com.klyx.ui.components.KxTextField
import com.klyx.ui.theme.bodySmall
import com.klyx.ui.theme.colors
import com.klyx.ui.theme.surfaceVariant
import com.klyx.ui.theme.textMuted
import com.klyx.ui.theme.textPrimary
import com.klyx.ui.theme.typography
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun rememberRecentFiles(): List<Document> {
    val workspace: Workspace = koinInject()
    val groups by workspace.editorGroups.collectAsState()
    val groupId by workspace.activeEditorGroupId.collectAsState()

    val current = groups.find { it.id == groupId }?.activeDocumentId
    val history = remember { mutableStateListOf<Document>() }

    LaunchedEffect(current) {
        workspace.document(current ?: return@LaunchedEffect)?.let {
            history.removeAll { previous -> previous.location == it.location }
            history.add(0, it)
            if (history.size > 50) history.removeAt(history.lastIndex)
        }
    }
    return history.toList()
}

@Composable
internal fun Palette(
    mode: EditorCommand,
    recent: List<Document>,
    bindings: Map<String, String>,
    run: (EditorCommand) -> Unit,
    dismiss: () -> Unit,
) = key(mode) {
    val workspace: Workspace = koinInject()
    val projectId by workspace.activeProjectId.collectAsState()
    val projects by workspace.projects.collectAsState()
    val project = projects[projectId]

    val documents by workspace.documents.collectAsState()

    val field = rememberTextFieldState()
    val find = mode == EditorCommand.FindInFiles
    val query = if (find) field.text.toString() else field.text.toString().trim()

    val editors: EditorStore = koinInject()
    val lsp: EditorLsp = koinInject()

    val commands = EditorCommand.entries.filter {
        it != EditorCommand.Palette && matchRank(it.title, it.title, query) != null
    }
    val quick = mode == EditorCommand.QuickOpen
    var index by remember(project) { mutableStateOf<List<FileMatch>?>(null) }

    LaunchedEffect(mode, project) {
        if (project == null) return@LaunchedEffect
        if (quick) projectFiles(project).collect { index = it }
        if (find) index = projectFiles(project).last()
    }

    var lines by remember(query, project) {
        mutableStateOf(emptyList<LineMatch>())
    }
    var searching by remember(query, project) {
        mutableStateOf(find && project != null && query.isNotEmpty())
    }

    if (find && project != null && query.isNotEmpty()) {
        val buffers by workspace.buffers.collectAsState()

        LaunchedEffect(query, project, index, documents, buffers) {
            val files = index
            searching = true
            delay(150.milliseconds)
            val overlays = searchBuffers(project, documents.values, buffers)
            searchLines(files.orEmpty(), overlays, query).collect { lines = it }
            searching = files == null
        }
    }

    val history = remember(recent, project, documents) {
        (recent + documents.values)
            .distinctBy { it.location }
            .filter { it.isVisibleIn(projectId) }
            .map {
                val file = workspace.resolve(it)
                FileMatch(
                    path = (it.location as? DocumentLocation.Project)?.relativePath?.toString() ?: file.path,
                    name = it.name,
                    file = file,
                    location = it.location
                )
            }
    }

    var matches by remember(query, project) {
        mutableStateOf(emptyList<FileMatch>())
    }

    LaunchedEffect(query, index, history) {
        if (quick) {
            matches = withContext(Dispatchers.Default) {
                val candidates = (history + index.orEmpty()).distinctBy { it.file.path }

                if (query.isEmpty()) {
                    candidates.take(100)
                } else {
                    candidates.mapNotNull { file ->
                        ensureActive()
                        matchRank(file.path, file.name, query)?.let { it to file }
                    }.sortedWith(
                        compareBy({ it.first }, { it.second.path.length }, { it.second.path })
                    ).take(100).map { it.second }
                }
            }
        }
    }

    val count = when {
        find -> lines.size
        quick -> matches.size
        else -> commands.size
    }

    var selected by remember(query) { mutableIntStateOf(0) }
    val list = rememberLazyListState()
    val focus = remember { FocusRequester() }

    fun choose(i: Int) {
        if (find || quick) {
            val hit = if (find) lines.getOrNull(i) else null
            val file = hit?.file ?: matches.getOrNull(i) ?: return

            val existing = workspace.find(file.location)
                ?: documents.values.find { workspace.resolve(it).path == file.file.path }

            if (hit != null || existing == null || existing.id != editors.activeDocumentId.value) lsp.rememberCursor()
            dismiss()

            if (hit == null) {
                action(existing?.let { ActivateDocument(it.id) } ?: OpenDocument(file.location, file.name))()
            } else {
                val id = existing?.id
                    ?.also { workspace.activateDocument(it) }
                    ?: workspace.openDocument(
                        location = file.location,
                        name = file.name
                    )
                editors.pendingJump = DocumentJump(id, hit.line, hit.column)
            }
        } else {
            commands.getOrNull(i)?.let {
                dismiss()
                run(it)
            }
        }
    }

    KxDialog(visible = true, onDismissRequest = dismiss, title = mode.title) {
        LaunchedEffect(Unit) {
            focus.requestFocus()
        }

        LaunchedEffect(selected, query) {
            list.scrollToItem(selected)
        }

        Column(
            modifier = Modifier.onPreviewKeyEvent {
                if (it.key !in listOf(
                        Key.DirectionUp,
                        Key.DirectionDown,
                        Key.Enter,
                        Key.NumPadEnter,
                        Key.Escape
                    )
                ) return@onPreviewKeyEvent false

                if (it.type == KeyEventType.KeyDown) when (it.key) {
                    Key.Escape -> dismiss()
                    Key.Enter, Key.NumPadEnter -> choose(selected)
                    Key.DirectionDown -> selected = (selected + 1).coerceAtMost((count - 1).coerceAtLeast(0))
                    Key.DirectionUp -> selected = (selected - 1).coerceAtLeast(0)
                }
                true
            }
        ) {
            KxTextField(
                state = field,
                modifier = Modifier.focusRequester(focus),
                placeholder = when {
                    find -> "Text to find"
                    quick -> "File name or path"
                    else -> "Search commands"
                }
            )

            LazyColumn(
                state = list,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                items(count) { i ->
                    val title = when {
                        find -> lines[i].let { (file, line) -> "${file.path}:${line + 1}" }
                        quick -> matches[i].name
                        else -> commands[i].title
                    }
                    val detail = when {
                        find -> lines[i].preview
                        quick -> matches[i].path
                        else -> commands[i].binding(bindings)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (i == selected) Modifier.background(Theme[colors][surfaceVariant]) else Modifier)
                            .clickable { choose(i) }
                            .padding(8.dp)
                    ) {
                        Text(title, color = Theme[colors][textPrimary])

                        if (detail.isNotEmpty()) {
                            Text(
                                text = detail,
                                color = Theme[colors][textMuted],
                                style = Theme[typography][bodySmall]
                            )
                        }
                    }
                }

                if (count == 0) {
                    item {
                        Text(
                            text = when {
                                find && project == null -> "Open a project to search its files."
                                find && query.isEmpty() -> "Enter text to search the project."
                                searching || (quick && project != null && index == null) -> "Searching project…"
                                quick && project == null && history.isEmpty() -> "Open a project to find files."
                                else -> "No matches"
                            },
                            color = Theme[colors][textMuted]
                        )
                    }
                }

                if (find && count == matchLimit) {
                    item {
                        Text("Showing the first $matchLimit matching lines", color = Theme[colors][textMuted])
                    }
                }
            }
        }
    }
}
