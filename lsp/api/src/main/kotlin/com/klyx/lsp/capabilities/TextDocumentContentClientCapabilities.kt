package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Client capabilities for a text document content provider.
 *
 * @since 3.18.0
 */
@Serializable
data class TextDocumentContentClientCapabilities(
    /**
     * Text document content provider supports dynamic registration.
     */
    override var dynamicRegistration: Boolean? = null
) : DynamicRegistrationCapabilities
