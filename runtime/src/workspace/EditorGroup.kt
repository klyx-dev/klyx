package com.klyx.runtime.workspace

import androidx.compose.runtime.Immutable

data class EditorGroup(
    val id: EditorGroupId,
    val documents: List<WorkspaceDocument> = emptyList(),
    val activeDocumentId: DocumentId? = null,
)

@Immutable
data class WorkspaceDocument(
    val documentId: DocumentId,
    val pinned: Boolean = false,
    val preview: Boolean = false,
)
