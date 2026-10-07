package com.klyx.runtime.workspace

data class Document(
    val id: DocumentId,
    val name: String,
    val location: DocumentLocation,
)

fun Document.isVisibleIn(activeProjectId: ProjectId?): Boolean = when (location) {
    is DocumentLocation.Project -> location.projectId == activeProjectId
    else -> true
}
