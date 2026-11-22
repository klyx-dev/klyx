package com.klyx.runtime.workspace

import com.klyx.core.Disposable
import com.klyx.core.action.Action
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    // Serialize workspace transitions with editor callbacks and close confirmation.
    private inline fun <reified A : Action> workspaceAction(noinline handler: (A) -> Unit) =
        action<A> { request -> withContext(Dispatchers.Main.immediate) { handler(request) } }

    override fun registerActions(): Disposable {
        val handlers = listOf(
            workspaceAction<SplitEditor> { workspace.splitEditor() },
            workspaceAction<ActivateEditorGroup> { workspace.activateEditorGroup(it.groupId) },
            workspaceAction<CloseEditorGroup> { workspace.closeEditorGroup(it.groupId) },
            workspaceAction<OpenDocument> { request ->
                workspace.openDocument(
                    location = request.location,
                    name = request.name,
                    preview = request.preview,
                    tabScope = currentTabScope(),
                )
            },
            workspaceAction<ActivateDocument> { workspace.activateDocument(it.documentId) },
            workspaceAction<CloseDocument> { workspace.closeDocument(it.documentId) },
            workspaceAction<PinDocument> { workspace.pinDocument(it.documentId) },
            workspaceAction<UnpinDocument> { workspace.unpinDocument(it.documentId) },
            workspaceAction<PromotePreviewDocument> { workspace.promotePreviewDocument(it.documentId) },
            workspaceAction<CloseActiveDocument> { workspace.closeActiveDocument(currentTabScope()) },
            workspaceAction<ActivateNextDocument> { workspace.activateAdjacentDocument(1, currentTabScope()) },
            workspaceAction<ActivatePreviousDocument> { workspace.activateAdjacentDocument(-1, currentTabScope()) },
            workspaceAction<OpenProject> { workspace.openProject(it.root) },
            workspaceAction<ActivateProject> { workspace.activateProject(it.projectId) },
            workspaceAction<CloseProject> { workspace.closeProject(it.projectId) },
            workspaceAction<AddProject> { workspace.addProject(it.projectId) },
            workspaceAction<RemoveProject> { workspace.removeProject(it.projectId) },
        )

        return Disposable { handlers.forEach { it.unregister() } }
    }
}
