package com.klyx.runtime.action

import com.klyx.core.action.Action
import com.klyx.core.action.ActionDescriptor
import com.klyx.core.action.ActionId
import com.klyx.runtime.workspace.ProjectId
import okio.Path

/** Opens a project, adds it to the workspace, and makes it the active project there. */
data class OpenProject(
    val root: Path,
) : Action {
    override val id = OpenProject.id

    companion object : ActionDescriptor<OpenProject> {
        override val id = ActionId("project.openProject")
    }
}

/** Makes one of the workspace's projects the current one. */
data class ActivateProject(val projectId: ProjectId) : Action {
    override val id = ActivateProject.id

    companion object : ActionDescriptor<ActivateProject> {
        override val id = ActionId("project.activateProject")
    }
}

/** Drops a project from the workspace, then forgets it. Open documents are left alone. */
data class CloseProject(val projectId: ProjectId) : Action {
    override val id = CloseProject.id

    companion object : ActionDescriptor<CloseProject> {
        override val id = ActionId("project.closeProject")
    }
}

/**
 * Puts an already open project into the workspace, without registering it twice.
 */
data class AddProject(
    val projectId: ProjectId,
) : Action {
    override val id = AddProject.id

    companion object : ActionDescriptor<AddProject> {
        override val id = ActionId("project.addProject")
    }
}

/** Takes a project out of the workspace while leaving its registration intact. */
data class RemoveProject(
    val projectId: ProjectId,
) : Action {
    override val id = RemoveProject.id

    companion object : ActionDescriptor<RemoveProject> {
        override val id = ActionId("project.removeProject")
    }
}
