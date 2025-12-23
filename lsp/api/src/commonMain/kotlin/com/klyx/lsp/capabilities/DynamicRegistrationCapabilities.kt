package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

@Serializable
sealed interface DynamicRegistrationCapabilities {
    /**
     * Supports dynamic registration.
     */
    val dynamicRegistration: Boolean? get() = null
}
