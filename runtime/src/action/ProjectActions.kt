package com.klyx.runtime.action

import com.klyx.core.action.Action
import com.klyx.runtime.workspace.ProjectId
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.serialization.Serializable

/** Opens a project, adds it to the workspace, and makes it the active project there. */
@Serializable
data class OpenProject(val root: PlatformFile) : Action

/** Makes one of the workspace's projects the current one. */
@Serializable
data class ActivateProject(val projectId: ProjectId) : Action

/** Drops a project from the workspace, then forgets it. Open documents are left alone. */
@Serializable
data class CloseProject(val projectId: ProjectId) : Action

/**
 * Puts an already open project into the workspace, without registering it twice.
 */
@Serializable
data class AddProject(val projectId: ProjectId) : Action

/** Takes a project out of the workspace while leaving its registration intact. */
@Serializable
data class RemoveProject(val projectId: ProjectId) : Action
