package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Text document content provider options.
 *
 * @since 3.18.0
 */
@Serializable
data class TextDocumentContentOptions(
    /**
     * The schemes for which the server provides content.
     */
    val schemes: List<String>
)

/**
 * Text document content provider registration options.
 *
 * @since 3.18.0
 */
@Serializable
data class TextDocumentContentRegistrationOptions(
    /**
     * The schemes for which the server provides content.
     */
    val schemes: List<String>,
    override val id: String?
) : StaticRegistrationOptions
