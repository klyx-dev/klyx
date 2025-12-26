package com.klyx.lsp.capabilities

import com.klyx.lsp.PositionEncodingKind
import kotlinx.serialization.Serializable

/**
 * General client capabilities.
 *
 * @since 3.16.0
 */
@Serializable
data class GeneralClientCapabilities(
    /**
     * Client capability that signals how the client
     * handles stale requests (e.g. a request
     * for which the client will not process the response
     * anymore since the information is outdated).
     *
     * @since 3.17.0
     */
    var staleRequestSupport: StaleRequestSupportCapabilities? = null,

    /**
     * Client capabilities specific to regular expressions.
     *
     * @since 3.16.0
     */
    var regularExpressions: RegularExpressionsCapabilities? = null,

    /**
     * Client capabilities specific to the client's markdown parser.
     *
     * @since 3.16.0
     */
    var markdown: MarkdownCapabilities? = null,

    /**
     * The position encodings supported by the client. Client and server
     * have to agree on the same position encoding to ensure that offsets
     * (e.g. character position in a line) are interpreted the same on both
     * side.
     *
     * To keep the protocol backwards compatible the following applies: if
     * the value [PositionEncodingKind.UTF16] is missing from the array of position encodings
     * servers can assume that the client supports `UTF-16`. `UTF-16` is
     * therefore a mandatory encoding.
     *
     * If omitted it defaults to `listOf(PositionEncodingKind.UTF16)`.
     *
     * Implementation considerations: since the conversion from one encoding
     * into another requires the content of the file / line the conversion
     * is best done where the file is read which is usually on the server
     * side.
     *
     * @since 3.17.0
     */
    var positionEncodings: List<PositionEncodingKind>? = null
)

/**
 * Client capability that signals how the client
 * handles stale requests (e.g. a request
 * for which the client will not process the response
 * anymore since the information is outdated).
 *
 * @since 3.17.0
 */
@Serializable
data class StaleRequestSupportCapabilities(
    /**
     * The client will actively cancel the request.
     */
    val cancel: Boolean,

    /**
     * The list of requests for which the client
     * will retry the request if it receives a
     * response with error code `ContentModified``
     */
    val retryOnContentModified: List<String>
)

