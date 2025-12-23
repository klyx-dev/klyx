package com.klyx.lsp.capabilities

import com.klyx.lsp.CompletionItemTag
import com.klyx.lsp.InsertTextMode
import com.klyx.lsp.MarkupKind
import kotlinx.serialization.Serializable

/**
 * The client supports the following `CompletionItem` specific
 * capabilities.
 */
@Serializable
data class CompletionItemClientCapabilities(
    /**
     * Client supports snippets as insert text.
     *
     * A snippet can define tab stops and placeholders with `$1`, `$2`
     * and `${3:foo}`. `$0` defines the final tab stop, it defaults to
     * the end of the snippet. Placeholders with equal identifiers are
     * linked, that is, typing in one will update others too.
     */
    var snippetSupport: Boolean? = null,

    /**
     * Client supports commit characters on a completion item.
     */
    var commitCharactersSupport: Boolean? = null,

    /**
     * Client supports these content formats for the documentation
     * property. The order describes the preferred format of the client.
     */
    var documentationFormat: List<MarkupKind>? = null,

    /**
     * Client supports the deprecated property on a completion item.
     */
    var deprecatedSupport: Boolean? = null,

    /**
     * Client supports the preselect property on a completion item.
     */
    var preselectSupport: Boolean? = null,

    /**
     * Client supports the tag property on a completion item. Clients
     * supporting tags have to handle unknown tags gracefully. Clients
     * especially need to preserve unknown tags when sending a completion
     * item back to the server in a resolve call.
     *
     * @since 3.15.0
     */
    var tagSupport: CompletionItemTagSupportClientCapabilities? = null,

    /**
     * Client supports insert replace edit to control different behavior if
     * a completion item is inserted in the text or should replace text.
     *
     * @since 3.16.0
     */
    var insertReplaceSupport: Boolean? = null,

    /**
     * Indicates which properties a client can resolve lazily on a
     * completion item. Before version 3.16.0, only the predefined properties
     * `documentation` and `detail` could be resolved lazily.
     *
     * @since 3.16.0
     */
    var resolveSupport: CompletionItemResolveSupportClientCapabilities? = null,

    /**
     * The client supports the `insertTextMode` property on
     * a completion item to override the whitespace handling mode
     * as defined by the client (see `insertTextMode`).
     *
     * @since 3.16.0
     */
    var insertTextModeSupport: InsertTextModeSupportClientCapabilities? = null,

    /**
     * The client has support for completion item label
     * details (see also `CompletionItemLabelDetails`).
     *
     * @since 3.17.0
     */
    var labelDetailsSupport: Boolean? = null
)

/**
 * Client supports the tag property on a completion item. Clients
 * supporting tags have to handle unknown tags gracefully. Clients
 * especially need to preserve unknown tags when sending a completion
 * item back to the server in a resolve call.
 *
 * @since 3.15.0
 */
@Serializable
data class CompletionItemTagSupportClientCapabilities(
    /**
     * The tags supported by the client.
     */
    val valueSet: List<CompletionItemTag>
)

/**
 * Indicates which properties a client can resolve lazily on a
 * completion item. Before version 3.16.0, only the predefined properties
 * `documentation` and `detail` could be resolved lazily.
 *
 * @since 3.16.0
 */
@Serializable
data class CompletionItemResolveSupportClientCapabilities(
    /**
     * The properties that a client can resolve lazily.
     */
    val properties: List<String>
)

/**
 * The client supports the `insertTextMode` property on
 * a completion item to override the whitespace handling mode
 * as defined by the client (see `insertTextMode`).
 *
 * @since 3.16.0
 */
@Serializable
data class InsertTextModeSupportClientCapabilities(val valueSet: List<InsertTextMode>)
