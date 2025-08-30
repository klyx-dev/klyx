package com.klyx.extension.api.lsp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * The settings for a particular language server.
 *
 * @property binary The settings for the language server binary.
 * @property initializationOptions The initialization options to pass to the language server.
 * @property settings The settings to pass to language server.
 */
@Serializable
data class LspSettings(
    val binary: CommandSettings? = null,
    val initializationOptions: JsonElement? = null,
    val settings: JsonElement? = null
)

/**
 * The settings for a command.
 *
 * @property path The path to the command.
 * @property arguments The arguments to pass to the command.
 * @property env The environment variables.
 */
@Serializable
data class CommandSettings(
    val path: String? = null,
    val arguments: List<String>? = null,
    val env: Map<String, String>? = null
)
