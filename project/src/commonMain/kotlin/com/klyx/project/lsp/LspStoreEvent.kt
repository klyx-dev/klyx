package com.klyx.project.lsp

import com.klyx.core.lsp.LanguageServerId
import com.klyx.core.lsp.LanguageServerName
import com.klyx.settings.WorktreeId

sealed interface LspStoreEvent {
    data class LanguageServerAdded(
        val serverId: LanguageServerId,
        val serverName: LanguageServerName,
        val worktreeId: WorktreeId
    ) : LspStoreEvent

    data class LanguageServerRemoved(val serverId: LanguageServerId) : LspStoreEvent

    data class LanguageServerUpdate(val serverId: LanguageServerId) : LspStoreEvent
}
