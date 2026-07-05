package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

@Serializable
data class ResolveSupportCapabilities(
    /**
     * The properties that a client can resolve lazily.
     */
    val properties: List<String>
)
