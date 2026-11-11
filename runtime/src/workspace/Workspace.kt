@file:OptIn(ExperimentalAtomicApi::class)

package com.klyx.runtime.workspace

import androidx.datastore.core.DataStore
import com.klyx.core.Disposable
import com.klyx.core.Log
import com.klyx.core.action.action
import com.klyx.runtime.Platform
import com.klyx.runtime.action.ActivateDocument
import com.klyx.runtime.action.ActivateEditorGroup
import com.klyx.runtime.action.ActivateNextDocument
import com.klyx.runtime.action.ActivatePreviousDocument
import com.klyx.runtime.action.ActivateProject
import com.klyx.runtime.action.AddProject
import com.klyx.runtime.action.CloseActiveDocument
import com.klyx.runtime.action.CloseDocument
import com.klyx.runtime.action.CloseEditorGroup
import com.klyx.runtime.action.CloseProject
import com.klyx.runtime.action.OpenDocument
import com.klyx.runtime.action.OpenProject
import com.klyx.runtime.action.PinDocument
import com.klyx.runtime.action.PromotePreviewDocument
import com.klyx.runtime.action.RemoveProject
import com.klyx.runtime.action.SplitEditor
import com.klyx.runtime.action.UnpinDocument
import com.klyx.runtime.currentPlatform
import com.klyx.runtime.settings.CustomLspServer
import com.klyx.runtime.settings.Settings
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.parent
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.resolve
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.annotation.Single
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi

@Single
class Workspace(private val settingsStore: DataStore<Settings>? = null) {

    private val settingsScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        settingsStore?.let { store ->
            settingsScope.launch {
                store.data.collect { settings ->
                    tabScope.value = if (settings.showAllProjectsTabs) {
                        EditorTabScope.AllProjects
                    } else {
                        EditorTabScope.CurrentProject
                    }
                    showUndoRedoButtons.value = settings.showUndoRedoButtons
                    showInlayHints.value = settings.showInlayHints
                    autoSave.value = settings.autoSave
                    roundedUi.value = settings.roundedUi
                    cornerRadius.value = settings.cornerRadius
                    customLspServers.value = settings.customLspServers
                }
            }
        }
    }

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

    val tabScope: StateFlow<EditorTabScope>
        field = MutableStateFlow(EditorTabScope.CurrentProject)

    private var nextGroupNumber = AtomicInt(1)

    fun setTabScope(scope: EditorTabScope) {
        tabScope.update { scope }
        persistSettings { it.copy(showAllProjectsTabs = scope == EditorTabScope.AllProjects) }
    }

    val showUndoRedoButtons: StateFlow<Boolean>
        field = MutableStateFlow(currentPlatform != Platform.JVM)

    fun setShowUndoRedoButtons(show: Boolean) {
        showUndoRedoButtons.update { show }
        persistSettings { it.copy(showUndoRedoButtons = show) }
    }

    val showInlayHints: StateFlow<Boolean>
        field = MutableStateFlow(true)

    fun setShowInlayHints(show: Boolean) {
        showInlayHints.update { show }
        persistSettings { it.copy(showInlayHints = show) }
    }

    val autoSave: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setAutoSave(save: Boolean) {
        autoSave.update { save }
        persistSettings { it.copy(autoSave = save) }
    }

    val roundedUi: StateFlow<Boolean>
        field = MutableStateFlow(false)

    fun setRoundedUi(rounded: Boolean) {
        roundedUi.update { rounded }
        persistSettings { it.copy(roundedUi = rounded) }
    }

    val cornerRadius: StateFlow<Int>
        field = MutableStateFlow(8)

    fun setCornerRadius(radius: Int) {
        val clamped = radius.coerceIn(0, 32)
        cornerRadius.update { clamped }
        persistSettings { it.copy(cornerRadius = clamped) }
    }

    val customLspServers: StateFlow<List<CustomLspServer>>
        field = MutableStateFlow(emptyList())

    fun addCustomLspServer(server: CustomLspServer) {
        customLspServers.update { current ->
            (current.filterNot { it.languageId == server.languageId } + server)
                .sortedBy { it.languageId }
        }
        persistSettings { it.copy(customLspServers = customLspServers.value) }
    }

    fun removeCustomLspServer(languageId: String) {
        customLspServers.update { current -> current.filterNot { it.languageId == languageId } }
        persistSettings { it.copy(customLspServers = customLspServers.value) }
    }

    private fun persistSettings(transform: (Settings) -> Settings) {
        settingsStore?.let { store ->
            settingsScope.launch { store.updateData(transform) }
        }
    }

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
        val id = EditorGroupId("group-${nextGroupNumber.fetchAndAdd(1)}")
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
    fun openDocument(location: DocumentLocation, name: String, preview: Boolean = false): DocumentId {
        Log.i("Workspace", "openDocument $name preview=$preview")
        val document = open(location, name)
        val groupId = activeGroupOrNull()?.id ?: createEditorGroup()
        if (preview) protectHiddenPreview(groupId)
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

    fun closeActiveDocument() {
        val group = activeGroup()
        val visible = visibleTabs(group)
        val id = group.activeDocumentId?.takeIf { selected -> visible.any { it.documentId == selected } }
            ?: visible.firstOrNull()?.documentId
            ?: return
        closeDocument(id)
    }

    fun activateAdjacentDocument(delta: Int) {
        val group = activeGroup()
        val tabs = visibleTabs(group)
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

    fun visibleTabs(group: EditorGroup): List<WorkspaceDocument> {
        if (tabScope.value == EditorTabScope.AllProjects) return group.documents
        val active = activeProjectId.value
        return group.documents.filter { tab -> document(tab.documentId)?.isVisibleIn(active) ?: true }
    }

    fun hasVisibleTabs(): Boolean {
        val docs = documents.value
        val active = activeProjectId.value
        val scope = tabScope.value
        return editorGroups.value.any { group ->
            group.documents.any { tab ->
                if (scope == EditorTabScope.AllProjects) true
                else docs[tab.documentId]?.isVisibleIn(active) ?: true
            }
        }
    }

    private fun protectHiddenPreview(groupId: EditorGroupId) {
        if (tabScope.value != EditorTabScope.CurrentProject) return
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

    @IgnorableReturnValue
    fun registerActions(): Disposable {
        val registrations = listOf(
            action<SplitEditor> { splitEditor() },
            action<ActivateEditorGroup> { activateEditorGroup(it.groupId) },
            action<CloseEditorGroup> { closeEditorGroup(it.groupId) },
            action<OpenDocument> { (location, name, preview) ->
                openDocument(location, name, preview)
            },
            action<ActivateDocument> { activateDocument(it.documentId) },
            action<CloseDocument> { closeDocument(it.documentId) },
            action<PinDocument> { pinDocument(it.documentId) },
            action<UnpinDocument> { unpinDocument(it.documentId) },
            action<PromotePreviewDocument> { promotePreviewDocument(it.documentId) },
            action<CloseActiveDocument> { closeActiveDocument() },
            action<ActivateNextDocument> { activateAdjacentDocument(1) },
            action<ActivatePreviousDocument> { activateAdjacentDocument(-1) },
            action<OpenProject> { openProject(it.root) },
            action<ActivateProject> { activateProject(it.projectId) },
            action<CloseProject> { closeProject(it.projectId) },
            action<AddProject> { addProject(it.projectId) },
            action<RemoveProject> { removeProject(it.projectId) },
        )

        return Disposable { registrations.forEach { it.unregister() } }
    }
}
