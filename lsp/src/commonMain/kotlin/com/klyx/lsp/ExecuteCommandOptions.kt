package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#executeCommandOptions)
 */
@Serializable
data class ExecuteCommandOptions(
    /**
     * The commands to be executed on the server.
     */
    val commands: List<String>
) : WorkDoneProgressOptions()

/**
 * Execute command registration options.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#executeCommandRegistrationOptions)
 */
@Serializable
data class ExecuteCommandRegistrationOptions(
    /**
     * The commands to be executed on the server.
     */
    val commands: List<String>
) : WorkDoneProgressOptions()
