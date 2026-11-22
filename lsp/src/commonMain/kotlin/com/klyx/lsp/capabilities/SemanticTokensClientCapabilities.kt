package com.klyx.lsp.capabilities

import com.klyx.lsp.TokenFormat
import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SemanticTokensClientCapabilities(
    /**
     * Whether the implementation supports dynamic registration. If this is set to
     * `true`, the client supports the new `(TextDocumentRegistrationOptions &
     * StaticRegistrationOptions)` return value for the corresponding server
     * capability as well.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * Which requests the client supports and might send to the server
     * depending on the server's capability. Please note that clients might not
     * show semantic tokens or degrade some of the user experience if a range
     * or full request is advertised by the client but not provided by the
     * server. If, for example, the client capability `requests.full` and
     * `request.range` are both set to true but the server only provides a
     * range provider, the client might not render a minimap correctly or might
     * even decide to not show any semantic tokens at all.
     */
    var requests: SemanticTokensClientCapabilitiesRequests = SemanticTokensClientCapabilitiesRequests(),

    /**
     * The token types that the client supports.
     */
    var tokenTypes: List<String> = emptyList(),

    /**
     * The token modifiers that the client supports.
     */
    var tokenModifiers: List<String> = emptyList(),

    /**
     * The formats the client supports.
     */
    var formats: List<TokenFormat> = emptyList(),

    /**
     * Whether the client supports tokens that can overlap each other.
     */
    var overlappingTokenSupport: Boolean? = null,

    /**
     * Whether the client supports tokens that can span multiple lines.
     */
    var multilineTokenSupport: Boolean? = null,

    /**
     * Whether the client allows the server to actively cancel a
     * semantic token request, e.g. supports returning
     * ErrorCodes.ServerCancelled. If a server does so, the client
     * needs to retrigger the request.
     *
     * @since 3.17.0
     */
    var serverCancelSupport: Boolean? = null,

    /**
     * Whether the client uses semantic tokens to augment existing
     * syntax tokens. If set to `true`, client side created syntax
     * tokens and semantic tokens are both used for colorization. If
     * set to `false`, the client only uses the returned semantic tokens
     * for colorization.
     *
     * If the value is `undefined` then the client behavior is not
     * specified.
     *
     * @since 3.17.0
     */
    var augmentsSyntaxTokens: Boolean? = null
) : DynamicRegistrationCapabilities

@Serializable
data class SemanticTokensClientCapabilitiesRequests(
    /**
     * The client will send the `textDocument/semanticTokens/range` request
     * if the server provides a corresponding handler.
     */
    var range: OneOf<Boolean, JsonObject>? = null,

    /**
     * The client will send the `textDocument/semanticTokens/full` request
     * if the server provides a corresponding handler.
     */
    var full: OneOf<Boolean, SemanticTokensClientCapabilitiesRequestsFull>? = null
)

@Serializable
data class SemanticTokensClientCapabilitiesRequestsFull(
    /**
     * The client will send the `textDocument/semanticTokens/full/delta`
     * request if the server provides a corresponding handler.
     */
    var delta: Boolean? = null
)
