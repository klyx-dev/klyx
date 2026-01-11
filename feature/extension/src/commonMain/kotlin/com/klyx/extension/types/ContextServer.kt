package com.klyx.extension.types

import kotlinx.serialization.json.JsonElement

/**
 * Configuration for context server setup and installation.
 */
data class ContextServerConfiguration(
    /**
     * Installation instructions in Markdown format.
     */
    val installationInstructions: String,

    /**
     * JSON schema for settings validation.
     */
    val settingsSchema: JsonElement,

    /**
     * Default settings template.
     */
    val defaultSettings: String,
)
