package com.klyx.lsp.capabilities

import com.klyx.lsp.FoldingRangeKind
import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#foldingRangeClientCapabilities)
 */
@Serializable
data class FoldingRangeClientCapabilities(
    /**
     * Whether implementation supports dynamic registration for folding range
     * providers. If this is set to `true` the client supports the new
     * `FoldingRangeRegistrationOptions` return value for the corresponding
     * server capability as well.
     */
    override var dynamicRegistration: Boolean? = null,

    /**
     * The maximum number of folding ranges that the client prefers to receive
     * per document. The value serves as a hint, servers are free to follow the
     * limit.
     */
    var rangeLimit: UInt? = null,

    /**
     * If set, the client signals that it only supports folding complete lines.
     * If set, client will ignore specified `startCharacter` and `endCharacter`
     * properties in a FoldingRange.
     */
    var lineFoldingOnly: Boolean? = null,

    /**
     * Specific options for the folding range kind.
     *
     * @since 3.17.0
     */
    var foldingRangeKind: FoldingRangeKindClientCapabilities? = null,

    /**
     * Specific options for the folding range.
     * @since 3.17.0
     */
    var foldingRange: FoldingRangeCapability? = null
) : DynamicRegistrationCapabilities

/**
 * Specific options for the folding range kind.
 *
 * @since 3.17.0
 */
@Serializable
data class FoldingRangeKindClientCapabilities(
    /**
     * The folding range kind values the client supports. When this
     * property exists the client also guarantees that it will
     * handle values outside its set gracefully and falls back
     * to a default value when unknown.
     */
    var valueSet: List<FoldingRangeKind>? = null
)

/**
 * Specific options for the folding range.
 * @since 3.17.0
 */
@Serializable
data class FoldingRangeCapability(
    /**
     * If set, the client signals that it supports setting collapsedText on
     * folding ranges to display custom labels instead of the default text.
     *
     * @since 3.17.0
     */
    var collapsedText: Boolean? = null
)
