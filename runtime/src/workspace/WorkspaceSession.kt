package com.klyx.runtime.workspace

import androidx.datastore.core.DataStore
import com.klyx.core.App
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okio.Path

/** A saved description of the workspace to open on the next launch. */
@Serializable
data class WorkspaceSessionSnapshot(
    val projectRoots: List<String> = emptyList(),
    val activeProjectRoot: String? = null,
    val groups: List<SavedEditorGroup> = emptyList(),
    val activeGroupIndex: Int? = null,
)

@Serializable
data class SavedEditorGroup(
    val tabs: List<SavedTab> = emptyList(),
    val activeTabIndex: Int? = null,
)

/** A null [projectRoot] means [path] is a standalone file or URI. */
@Serializable
data class SavedTab(
    val projectRoot: String? = null,
    val path: String,
    val name: String,
    val pinned: Boolean = false,
    val preview: Boolean = false,
)

@Serializable
data class WorkspaceSessionData(
    val snapshot: WorkspaceSessionSnapshot? = null,
)

expect fun createWorkspaceSessionDataStore(): DataStore<WorkspaceSessionData>

expect val App.workspaceSessionFilePath: Path

internal const val workspaceSessionFileName = "session.json"

internal val workspaceSessionJson = Json {
    encodeDefaults = true
    prettyPrint = true
    prettyPrintIndent = "  "
    ignoreUnknownKeys = true
    namingStrategy = JsonNamingStrategy.SnakeCase
}
