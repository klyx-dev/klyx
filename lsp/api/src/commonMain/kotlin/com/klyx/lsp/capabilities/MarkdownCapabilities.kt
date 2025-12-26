package com.klyx.lsp.capabilities

import kotlinx.serialization.Serializable

/**
 * Client capabilities specific to the used markdown parser.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#markdownClientCapabilities)
 *
 * @since 3.16.0
 */
@Serializable
data class MarkdownCapabilities(
    /**
     * The name of the parser.
     */
    val parser: String,

    /**
     * The version of the parser.
     */
    var version: String? = null,

    /**
     * A list of HTML tags that the client allows / supports in
     * Markdown.
     *
     * @since 3.17.0
     */
    var allowedTags: List<String>? = null
)
