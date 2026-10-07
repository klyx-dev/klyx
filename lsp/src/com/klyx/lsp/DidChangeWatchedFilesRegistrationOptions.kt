package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Describe options to be used when registering for file system change events.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#didChangeWatchedFilesRegistrationOptions)
 */
@Serializable
data class DidChangeWatchedFilesRegistrationOptions(
    /**
     * The watchers to register.
     */
    val watchers: List<FileSystemWatcher>
)
