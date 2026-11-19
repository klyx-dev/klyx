package com.klyx.lsp

import com.klyx.core.IoDispatcher
import com.klyx.core.utils.actualUri
import com.klyx.core.utils.relativeTo
import com.klyx.editor.DocumentJump
import com.klyx.runtime.workspace.Document
import com.klyx.runtime.workspace.DocumentLocation
import com.klyx.runtime.workspace.EditorGroupId
import com.klyx.runtime.workspace.ProjectId
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.resolve
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal expect fun fileForNavigationUri(uri: String): PlatformFile?

internal expect suspend fun resolveDefinitionAccess(uri: String, roots: List<PlatformFile>): PlatformFile?

internal data class CursorLocation(
    val document: Document,
    val project: ProjectId?,
    val group: EditorGroupId?,
    val line: Int,
    val column: Int,
)

private fun EditorLsp.cursorLocation(): CursorLocation? {
    val document = editorStore.activeDocumentId.value?.let(workspace::document) ?: return null
    (val line, val column) = editorStore.activeController?.getCursorPosition() ?: return null
    return CursorLocation(
        document = document,
        project = workspace.activeProjectId.value,
        group = workspace.activeEditorGroupId.value,
        line = line,
        column = column
    )
}

internal data class ReferenceResults(val origin: CursorLocation, val locations: List<Location>)

internal fun EditorLsp.goToDefinition() = requestNavigation(references = false)
internal fun EditorLsp.findReferences() = requestNavigation(references = true)

private fun EditorLsp.requestNavigation(references: Boolean) {
    navigationJob?.cancel()
    referenceResults.value = null
    val origin = cursorLocation() ?: return reportError("Open a file and place the cursor on a symbol.")

    navigationJob = scope.launch(Dispatchers.Main.immediate) {
        try {
            val document = origin.document
            val uri = LspDocumentUtils.uriOf(document, workspace)
            val text = editorStore.activeController?.getDocument()?.text ?: return@launch

            val session = withContext(Dispatchers.Default) {
                ensureSession(document)
            } ?: return@launch reportError("No language server is available for ${document.name}.")

            if (cursorLocation() != origin) return@launch

            val targets = withContext(Dispatchers.Default) {
                session.didChange(uri, text)
                if (references) {
                    session.references(uri, origin.line, origin.column).distinct()
                } else {
                    listOfNotNull(session.definition(uri, origin.line, origin.column))
                }
            }

            if (cursorLocation() != origin) return@launch
            if (targets.isEmpty()) return@launch reportError(if (references) "No references found." else "No definition found.")

            if (references) {
                referenceResults.value = ReferenceResults(origin, targets)
            } else {
                navigateToLocation(origin, targets.first())
            }
        } catch (failure: Exception) {
            if (failure is CancellationException) ensureActive()
            reportError("Language server navigation failed: ${failure.message}")
        }
    }
}

internal fun EditorLsp.selectReference(results: ReferenceResults, target: Location) {
    referenceResults.value = null
    navigationJob?.cancel()
    navigationJob = scope.launch(Dispatchers.Main.immediate) {
        try {
            navigateToLocation(results.origin, target)
        } catch (failure: Exception) {
            if (failure is CancellationException) ensureActive()
            reportError("Could not open reference: ${failure.message}")
        }
    }
}

private suspend fun EditorLsp.navigateToLocation(origin: CursorLocation, target: Location) {
    val targetFile = fileForNavigationUri(target.uri)
    val open = workspace.documents.value.values.firstOrNull { candidate ->
        val other = LspDocumentUtils.uriOfOrNull(candidate, workspace)
        other == target.uri || (targetFile != null && other != null && fileForNavigationUri(other)?.path == targetFile.path)
    }

    val location = open?.location ?: withContext(IoDispatcher) {
        definitionLocation(target.uri) ?: resolveDefinitionAccess(
            uri = target.uri,
            roots = workspace.projects.value.values.map { it.root }
        )?.let { DocumentLocation.Platform(it) }
    }

    if (cursorLocation() != origin) return
    if (location == null) return reportError("Klyx could not access the file at ${target.uri}")

    val name = open?.name ?: when (location) {
        is DocumentLocation.Platform -> location.file.name
        is DocumentLocation.Project -> location.relativePath.name
    }

    (val line, val character) = target.range.start
    if (location == origin.document.location && line.toInt() == origin.line && character.toInt() == origin.column) return

    reveal(location, name, line.toInt(), character.toInt())
    rememberCursor(origin)
}

internal fun EditorLsp.rememberCursor() {
    cursorLocation()?.let { rememberCursor(it) }
}

private fun EditorLsp.rememberCursor(origin: CursorLocation) {
    if (navigationHistory.lastOrNull() == origin) return
    navigationHistory.addLast(origin)
    if (navigationHistory.size > 100) navigationHistory.removeFirst()
}

private fun EditorLsp.definitionLocation(uri: String): DocumentLocation? {
    val file = fileForNavigationUri(uri) ?: return null

    for (project in workspace.projects.value.values) {
        val root = fileForNavigationUri(project.root.actualUri()) ?: continue
        val relative = runCatching { file.relativeTo(root) }.getOrNull() ?: continue

        if (relative.toString().replace('\\', '/').substringBefore('/') == "..") continue

        val isRegularFile = runCatching {
            project.root.resolve(relative.toString()).isRegularFile()
        }.getOrDefault(false)

        if (isRegularFile) return DocumentLocation.Project(project.id, relative)
    }
    return if (runCatching { file.isRegularFile() }.getOrDefault(false)) DocumentLocation.Platform(file) else null
}

private fun EditorLsp.reveal(location: DocumentLocation, name: String, line: Int, column: Int) {
    if (location is DocumentLocation.Project) workspace.activateProject(location.projectId)
    val open = workspace.find(location)
    val id = open?.id?.also { workspace.activateDocument(it) } ?: workspace.openDocument(location, name)
    editorStore.pendingJump = DocumentJump(id, line, column)
}

internal fun EditorLsp.goBack() {
    navigationJob?.cancel()
    while (navigationHistory.isNotEmpty()) {
        val previous = navigationHistory.removeLast()
        val project = previous.project
        if (project != null && project !in workspace.projectIds.value) continue

        val location = previous.document.location
        if (location is DocumentLocation.Project && location.projectId !in workspace.projectIds.value) continue

        if (project != null) workspace.activateProject(project)

        previous.group?.takeIf {
            id -> workspace.editorGroups.value.any { it.id == id }
        }?.let(workspace::activateEditorGroup)

        reveal(location, previous.document.name, previous.line, previous.column)
        return
    }
    reportError("No previous cursor position.")
}
