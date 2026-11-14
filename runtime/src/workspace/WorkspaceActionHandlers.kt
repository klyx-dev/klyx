package com.klyx.runtime.workspace

import com.klyx.core.Disposable
import com.klyx.core.action.ActionRegistrar
import com.klyx.core.action.action
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
import com.klyx.runtime.settings.SettingsStore
import org.koin.core.annotation.Single

@Single
class WorkspaceActionHandlers(
    private val workspace: Workspace,
    private val settings: SettingsStore,
) : ActionRegistrar {

    private fun currentTabScope(): EditorTabScope {
        return if (settings.settings.value.showAllProjectsTabs) {
            EditorTabScope.AllProjects
        } else {
            EditorTabScope.CurrentProject
        }
    }

    override fun registerActions(): Disposable {
        val handlers = listOf(
            action<SplitEditor> { workspace.splitEditor() },
            action<ActivateEditorGroup> { workspace.activateEditorGroup(it.groupId) },
            action<CloseEditorGroup> { workspace.closeEditorGroup(it.groupId) },
            action<OpenDocument> { request ->
                workspace.openDocument(
                    location = request.location,
                    name = request.name,
                    preview = request.preview,
                    tabScope = currentTabScope(),
                )
            },
            action<ActivateDocument> { workspace.activateDocument(it.documentId) },
            action<CloseDocument> { workspace.closeDocument(it.documentId) },
            action<PinDocument> { workspace.pinDocument(it.documentId) },
            action<UnpinDocument> { workspace.unpinDocument(it.documentId) },
            action<PromotePreviewDocument> { workspace.promotePreviewDocument(it.documentId) },
            action<CloseActiveDocument> { workspace.closeActiveDocument(currentTabScope()) },
            action<ActivateNextDocument> { workspace.activateAdjacentDocument(1, currentTabScope()) },
            action<ActivatePreviousDocument> { workspace.activateAdjacentDocument(-1, currentTabScope()) },
            action<OpenProject> { workspace.openProject(it.root) },
            action<ActivateProject> { workspace.activateProject(it.projectId) },
            action<CloseProject> { workspace.closeProject(it.projectId) },
            action<AddProject> { workspace.addProject(it.projectId) },
            action<RemoveProject> { workspace.removeProject(it.projectId) },
        )

        return Disposable { handlers.forEach { it.unregister() } }
    }
}
