package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * The options to register for file operations.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#fileOperationRegistrationOptions)
 *
 * @since 3.16.0
 */
@Serializable
data class FileOperationRegistrationOptions(
    /**
     * The actual filters.
     */
    val filters: List<FileOperationFilter>
)
