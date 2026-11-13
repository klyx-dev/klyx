@file:OptIn(ExperimentalAtomicApi::class)

package com.klyx.runtime.workspace

import com.klyx.core.Log
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.resolve
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Single
class Workspace {

    val documents: StateFlow<Map<DocumentId, Document>>
        field = MutableStateFlow(emptyMap())

    val projects: StateFlow<Map<ProjectId, Project>>
        field = MutableStateFlow(emptyMap())

    val projectIds: StateFlow<List<ProjectId>>
        field = MutableStateFlow(emptyList())

    val activeProjectId: StateFlow<ProjectId?>
        field = MutableStateFlow(null)

    val editorGroups: StateFlow<List<EditorGroup>>
        field = MutableStateFlow(emptyList())

    val activeEditorGroupId: StateFlow<EditorGroupId?>
        field = MutableStateFlow(null)

    private var nextGroupNumber = AtomicInt(1)

    internal fun observeSessionChanges(): Flow<WorkspaceSessionSnapshot> {
        val workspaceChanges = combine(
            projects,
            projectIds,
            activeProjectId,
            editorGroups,
            activeEditorGroupId,
        ) { _, _, _, _, _ -> Unit }
        return workspaceChanges.combine(documents) { _, _ -> createSessionSnapshot() }
            .distinctUntilChanged()
    }

    internal fun createSessionSnapshot(): WorkspaceSessionSnapshot {
        val projectsById = projects.value
        val groupList = editorGroups.value
        val roots = linkedSetOf<String>()
        projectIds.value.forEach { id -> projectsById[id]?.let { roots += it.root.path } }

        val sessionGroups = groupList.map { group ->
            var tabIndex = 0
            val tabIndexes = mutableMapOf<DocumentId, Int>()
            val tabs = group.documents.mapNotNull { workspaceDocument ->
                val document = documents.value[workspaceDocument.documentId] ?: return@mapNotNull null
                val tab = when (val location = document.location) {
                    is DocumentLocation.Project -> {
                        val root = projectsById[location.projectId]?.root?.path ?: return@mapNotNull null
                        roots += root
                        SavedTab(
                            projectRoot = root,
                            path = location.relativePath.toString(),
                            name = document.name,
                            pinned = workspaceDocument.pinned,
                            preview = workspaceDocument.preview,
                        )
                    }

                    is DocumentLocation.Platform -> SavedTab(
                        projectRoot = null,
                        path = location.file.path,
                        name = document.name,
                        pinned = workspaceDocument.pinned,
                        preview = workspaceDocument.preview,
                    )
                }
                tabIndexes[document.id] = tabIndex++
                tab
            }
            val activeTabIndex = group.activeDocumentId?.let(tabIndexes::get)
            SavedEditorGroup(tabs = tabs, activeTabIndex = activeTabIndex)
        }

        val activeProjectRoot = activeProjectId.value?.let { projectsById[it]?.root?.path }
        val activeGroupIndex = activeEditorGroupId.value?.let { id ->
            groupList.indexOfFirst { it.id == id }.takeIf { it >= 0 }
        }
        return WorkspaceSessionSnapshot(
            projectRoots = roots.toList(),
            activeProjectRoot = activeProjectRoot,
            groups = sessionGroups,
            activeGroupIndex = activeGroupIndex,
        )
    }

    internal fun restoreSession(snapshot: WorkspaceSessionSnapshot) {
        val focusBeforeRestore = currentFocus()
        val restoredProjects = restoreProjects(snapshot.projectRoots)
        val restoredGroups = snapshot.groups.map { restoreGroup(it, restoredProjects) }

        restoreFocus(snapshot, restoredProjects, restoredGroups, focusBeforeRestore)
    }

    private fun currentFocus() = WorkspaceFocus(
        hasOpenTabs = editorGroups.value.any { it.documents.isNotEmpty() },
        projectId = activeProjectId.value,
        groupId = activeEditorGroupId.value,
        documentId = activeGroupOrNull()?.activeDocumentId,
    )

    private fun restoreProjects(rootPaths: List<String>): Map<String, ProjectId> {
        val restoredProjects = linkedMapOf<String, ProjectId>()
        rootPaths.forEach { rootPath ->
            try {
                restoreProject(rootPath)?.let { restoredProjects[rootPath] = it }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("Workspace", "Skipping unavailable project $rootPath", error)
            }
        }
        return restoredProjects
    }

    private fun restoreProject(rootPath: String): ProjectId? {
        val root = PlatformFile(rootPath)
        if (!root.exists() || !root.isDirectory()) return null

        val projectId = ProjectId(root.toString())
        if (projectId in projects.value) {
            addProject(projectId)
        } else {
            openProject(root)
        }
        return projectId
    }

    private fun restoreGroup(
        savedGroup: SavedEditorGroup,
        restoredProjects: Map<String, ProjectId>,
    ): EditorGroupId {
        val groupId = createEditorGroup()
        val restoredTabs = mutableMapOf<Int, DocumentId>()

        savedGroup.tabs.forEachIndexed { index, tab ->
            try {
                restoreTab(groupId, tab, restoredProjects)?.let { restoredTabs[index] = it }
            } catch (error: kotlinx.coroutines.CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.w("Workspace", "Skipping unavailable document ${tab.path}", error)
            }
        }

        val activeDocument = savedGroup.activeTabIndex?.let(restoredTabs::get)
            ?: restoredTabs.values.firstOrNull()
        activeDocument?.let { activateDocument(groupId, it) }
        return groupId
    }

    private fun restoreTab(
        groupId: EditorGroupId,
        savedTab: SavedTab,
        restoredProjects: Map<String, ProjectId>,
    ): DocumentId? {
        val location = savedTab.toLocation(restoredProjects) ?: return null
        val file = fileFor(location) ?: return null
        if (!file.exists() || file.isDirectory()) return null

        val document = open(location, savedTab.name)
        openDocument(groupId, document.id, savedTab.preview)
        if (savedTab.pinned) pinDocument(groupId, document.id)
        return document.id
    }

    private fun SavedTab.toLocation(projectsByRoot: Map<String, ProjectId>): DocumentLocation? {
        if (projectRoot == null) return DocumentLocation.Platform(PlatformFile(path))

        val projectId = projectsByRoot[projectRoot] ?: return null
        return DocumentLocation.Project(projectId, kotlinx.io.files.Path(path))
    }

    private fun fileFor(location: DocumentLocation): PlatformFile? = when (location) {
        is DocumentLocation.Platform -> location.file
        is DocumentLocation.Project -> projects.value[location.projectId]
            ?.root?.resolve(location.relativePath.toString())
    }

    private fun restoreFocus(
        snapshot: WorkspaceSessionSnapshot,
        restoredProjects: Map<String, ProjectId>,
        restoredGroups: List<EditorGroupId>,
        previous: WorkspaceFocus,
    ) {
        val projectToActivate = previous.projectId?.takeIf { it in projectIds.value }
            ?: snapshot.activeProjectRoot
                ?.let(restoredProjects::get)
                ?.takeIf { it in projectIds.value }
        activeProjectId.update { projectToActivate }

        val groupToActivate = if (previous.hasOpenTabs) {
            previous.groupId?.takeIf { id -> editorGroups.value.any { it.id == id } }
        } else {
            snapshot.activeGroupIndex?.let(restoredGroups::getOrNull)
        }
        groupToActivate?.let { activateEditorGroup(it) }

        previous.documentId
            ?.takeIf { previous.hasOpenTabs && isDocumentOpen(it) }
            ?.let { activateDocument(it) }
    }

    private data class WorkspaceFocus(
        val hasOpenTabs: Boolean,
        val projectId: ProjectId?,
        val groupId: EditorGroupId?,
        val documentId: DocumentId?,
    )

    @IgnorableReturnValue
    fun openProject(root: PlatformFile): Project {
        Log.i("Workspace", "openProject ${root.path}")
        if (!root.exists()) error("Project root does not exist: $root")
        val project = Project(id = ProjectId(root.toString()), root = root)
        projects.update { it + (project.id to project) }
        addProject(project.id)
        activateProject(project.id)
        return project
    }

    fun addProject(projectId: ProjectId) {
        if (projectId in projectIds.value) {
            activeProjectId.update { projectId }
            return
        }
        projectIds.update { it + projectId }
        activeProjectId.update { projectId }
    }

    fun removeProject(projectId: ProjectId) {
        if (projectId !in projectIds.value) return
        val remaining = projectIds.value - projectId
        projectIds.update { remaining }
        if (activeProjectId.value == projectId) {
            activeProjectId.update { remaining.firstOrNull() }
        }
    }

    fun activateProject(projectId: ProjectId) {
        check(projectId in projectIds.value) { "Project ${projectId.value} is not part of the workspace" }
        activeProjectId.update { projectId }
    }

    fun closeProject(projectId: ProjectId) {
        removeProject(projectId)
        projects.update { it - projectId }
    }

    fun open(location: DocumentLocation, name: String): Document {
        find(location)?.let { return it }
        val document = Document(id = DocumentId(createDocumentId(location)), name = name, location = location)
        documents.update { it + (document.id to document) }
        return document
    }

    fun find(location: DocumentLocation): Document? =
        documents.value.values.firstOrNull { it.location == location }

    fun document(id: DocumentId): Document? = documents.value[id]

    operator fun get(id: DocumentId): Document =
        document(id) ?: error("Document not found: ${id.value}")

    operator fun get(id: ProjectId): Project =
        projects.value[id] ?: error("Project not found: ${id.value}")

    fun resolve(document: Document): PlatformFile = when (val location = document.location) {
        is DocumentLocation.Platform -> location.file
        is DocumentLocation.Project -> this[location.projectId].root.resolve(location.relativePath.toString())
    }

    suspend fun readText(id: DocumentId): String = withContext(Dispatchers.IO) {
        val document = get(id)
        resolve(document).readString()
    }

    suspend fun writeText(id: DocumentId, text: String): Unit = withContext(Dispatchers.IO) {
        val document = get(id)
        val file = resolve(document)
        file.parent()?.let { parent ->
            if (!parent.exists()) {
                parent.createDirectories()
            }
        }
        file.writeString(text)
        Log.d("Workspace", "write ${document.name} (${text.length} chars)")
    }

    fun createEditorGroup(): EditorGroupId {
        var id: EditorGroupId
        do {
            id = EditorGroupId("group-${nextGroupNumber.fetchAndAdd(1)}")
        } while (editorGroups.value.any { it.id == id })
        editorGroups.update { it + EditorGroup(id) }
        activeEditorGroupId.update { id }
        return id
    }

    fun closeEditorGroup(groupId: EditorGroupId) {
        if (editorGroups.value.size <= 1) return
        val remaining = editorGroups.value.filterNot { it.id == groupId }
        if (remaining.size == editorGroups.value.size) return
        editorGroups.update { remaining }
        if (activeEditorGroupId.value == groupId) {
            activeEditorGroupId.update { remaining.firstOrNull()?.id }
        }
        pruneDocuments()
    }

    fun activateEditorGroup(groupId: EditorGroupId) {
        check(editorGroups.value.any { it.id == groupId }) { "Group ${groupId.value} does not exist" }
        activeEditorGroupId.update { groupId }
    }

    fun activeGroupOrNull(): EditorGroup? =
        activeEditorGroupId.value?.let { id -> editorGroups.value.firstOrNull { it.id == id } }

    fun activeGroup(): EditorGroup =
        activeGroupOrNull() ?: error("Workspace has no active editor group")

    @IgnorableReturnValue
    fun openDocument(
        location: DocumentLocation,
        name: String,
        preview: Boolean = false,
        tabScope: EditorTabScope = EditorTabScope.CurrentProject,
    ): DocumentId {
        Log.i("Workspace", "openDocument $name preview=$preview")
        val document = open(location, name)
        val groupId = activeGroupOrNull()?.id ?: createEditorGroup()
        if (preview) protectHiddenPreview(groupId, tabScope)
        openDocument(groupId, document.id, preview)
        return document.id
    }

    fun openDocument(groupId: EditorGroupId, documentId: DocumentId, preview: Boolean = false) {
        updateGroup(groupId) { it.withOpened(documentId, preview) }
        pruneDocuments()
    }

    fun activateDocument(documentId: DocumentId) {
        val group = groupOf(documentId) ?: error("Document ${documentId.value} is not open")
        activateEditorGroup(group.id)
        updateGroup(group.id) { it.copy(activeDocumentId = documentId) }
    }

    fun activateDocument(groupId: EditorGroupId, documentId: DocumentId) {
        updateGroup(groupId) { it.withActivated(documentId) }
    }

    fun closeDocument(documentId: DocumentId) {
        val group = groupOf(documentId) ?: return
        updateGroup(group.id) { it.withClosed(documentId) }
        pruneDocuments()
    }

    fun closeDocument(groupId: EditorGroupId, documentId: DocumentId) {
        updateGroup(groupId) { it.withClosed(documentId) }
        pruneDocuments()
    }

    fun closeActiveDocument(tabScope: EditorTabScope = EditorTabScope.CurrentProject) {
        val group = activeGroup()
        val visible = visibleTabs(group, tabScope)
        val id = group.activeDocumentId?.takeIf { selected -> visible.any { it.documentId == selected } }
            ?: visible.firstOrNull()?.documentId
            ?: return
        closeDocument(id)
    }

    fun activateAdjacentDocument(
        delta: Int,
        tabScope: EditorTabScope = EditorTabScope.CurrentProject,
    ) {
        val group = activeGroup()
        val tabs = visibleTabs(group, tabScope)
        if (tabs.size < 2) return
        val current = group.activeDocumentId?.let { id -> tabs.indexOfFirst { it.documentId == id } } ?: -1
        val index = if (current < 0) 0 else (current + delta + tabs.size) % tabs.size
        updateGroup(group.id) { it.copy(activeDocumentId = tabs[index].documentId) }
    }

    fun pinDocument(documentId: DocumentId) {
        val group = groupOf(documentId) ?: error("Document ${documentId.value} is not open")
        updateGroup(group.id) { it.withUpdated(documentId) { copy(pinned = true, preview = false) } }
    }

    fun unpinDocument(documentId: DocumentId) {
        val group = groupOf(documentId) ?: error("Document ${documentId.value} is not open")
        updateGroup(group.id) { it.withUpdated(documentId) { copy(pinned = false) } }
    }

    fun promotePreviewDocument(documentId: DocumentId) {
        val group = groupOf(documentId) ?: error("Document ${documentId.value} is not open")
        updateGroup(group.id) { it.withUpdated(documentId, required = false) { copy(preview = false) } }
    }

    fun pinDocument(groupId: EditorGroupId, documentId: DocumentId) {
        updateGroup(groupId) { it.withUpdated(documentId) { copy(pinned = true, preview = false) } }
    }

    fun unpinDocument(groupId: EditorGroupId, documentId: DocumentId) {
        updateGroup(groupId) { it.withUpdated(documentId) { copy(pinned = false) } }
    }

    fun promotePreviewDocument(groupId: EditorGroupId, documentId: DocumentId) {
        updateGroup(groupId) { it.withUpdated(documentId, required = false) { copy(preview = false) } }
    }

    @IgnorableReturnValue
    fun splitEditor(): EditorGroupId = createEditorGroup()

    fun isDocumentOpen(documentId: DocumentId): Boolean = groupOf(documentId) != null

    fun isDocumentOpen(groupId: EditorGroupId, documentId: DocumentId): Boolean =
        editorGroups.value.firstOrNull { it.id == groupId }?.documents?.any { it.documentId == documentId } == true

    fun groupOf(documentId: DocumentId): EditorGroup? {
        val groups = editorGroups.value
        val focused = activeEditorGroupId.value
        return groups.firstOrNull { it.id == focused && it.documents.any { tab -> tab.documentId == documentId } }
            ?: groups.firstOrNull { group -> group.documents.any { tab -> tab.documentId == documentId } }
    }

    fun openDocumentIds(): Set<DocumentId> =
        editorGroups.value.flatMapTo(LinkedHashSet()) { group -> group.documents.map { it.documentId } }

    fun isAnyDocumentOpen(): Boolean = editorGroups.value.any { it.documents.isNotEmpty() }

    fun visibleTabs(group: EditorGroup, tabScope: EditorTabScope): List<WorkspaceDocument> {
        if (tabScope == EditorTabScope.AllProjects) return group.documents
        val active = activeProjectId.value
        return group.documents.filter { tab -> document(tab.documentId)?.isVisibleIn(active) ?: true }
    }

    fun hasVisibleTabs(tabScope: EditorTabScope): Boolean {
        val docs = documents.value
        val active = activeProjectId.value
        return editorGroups.value.any { group ->
            group.documents.any { tab ->
                if (tabScope == EditorTabScope.AllProjects) true
                else docs[tab.documentId]?.isVisibleIn(active) ?: true
            }
        }
    }

    private fun protectHiddenPreview(groupId: EditorGroupId, tabScope: EditorTabScope) {
        if (tabScope != EditorTabScope.CurrentProject) return
        val active = activeProjectId.value
        editorGroups.value.firstOrNull { it.id == groupId }
            ?.documents?.firstOrNull { it.preview && !it.pinned }
            ?.takeIf { tab -> document(tab.documentId)?.isVisibleIn(active) != true }
            ?.let { hidden -> updateGroup(groupId) { group -> group.withUpdated(hidden.documentId) { copy(preview = false) } } }
    }

    private fun pruneDocuments() {
        val open = openDocumentIds()
        if (open.size == documents.value.size) return
        documents.update { current -> current.filterKeys { it in open } }
    }

    private fun updateGroup(groupId: EditorGroupId, transform: (EditorGroup) -> EditorGroup) {
        val index = editorGroups.value.indexOfFirst { it.id == groupId }
        check(index >= 0) { "Group ${groupId.value} does not exist" }
        val groups = editorGroups.value.toMutableList()
        groups[index] = transform(groups[index])
        editorGroups.update { groups }
    }

    private fun createDocumentId(location: DocumentLocation): String = when (location) {
        is DocumentLocation.Project -> "${location.projectId.value}:${location.relativePath}"
        is DocumentLocation.Platform -> "platform:${location.file.path}"
    }

    private fun EditorGroup.withOpened(documentId: DocumentId, preview: Boolean): EditorGroup {
        val existing = documents.firstOrNull { it.documentId == documentId }
        if (existing != null) {
            if (existing.preview && !preview) {
                return copy(
                    activeDocumentId = documentId,
                    documents = documents.map { if (it.documentId == documentId) it.copy(preview = false) else it },
                )
            }
            return copy(activeDocumentId = documentId)
        }
        val retained = if (preview) documents.filterNot { it.preview && !it.pinned } else documents
        return copy(
            documents = retained + WorkspaceDocument(documentId = documentId, preview = preview),
            activeDocumentId = documentId,
        )
    }

    private fun EditorGroup.withActivated(documentId: DocumentId): EditorGroup {
        check(documents.any { it.documentId == documentId }) { "Document ${documentId.value} is not open" }
        return copy(activeDocumentId = documentId)
    }

    private fun EditorGroup.withClosed(documentId: DocumentId): EditorGroup {
        val index = documents.indexOfFirst { it.documentId == documentId }
        if (index < 0) return this
        val remaining = documents.toMutableList().apply { removeAt(index) }
        return copy(
            documents = remaining,
            activeDocumentId = if (activeDocumentId != documentId) {
                activeDocumentId
            } else {
                remaining.getOrNull(index)?.documentId ?: remaining.lastOrNull()?.documentId
            },
        )
    }

    private fun EditorGroup.withUpdated(
        documentId: DocumentId,
        required: Boolean = true,
        transform: WorkspaceDocument.() -> WorkspaceDocument,
    ): EditorGroup {
        val current = documents.firstOrNull { it.documentId == documentId }
        if (current == null) {
            check(!required) { "Document ${documentId.value} is not open" }
            return this
        }
        val next = current.transform()
        if (next == current) return this
        return copy(documents = documents.map { if (it.documentId == documentId) next else it })
    }

}
