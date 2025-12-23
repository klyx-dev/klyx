package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * Represents a folding range. To be valid, start and end line must be bigger
 * than zero and smaller than the number of lines in the document. Clients
 * are free to ignore invalid ranges.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#foldingRange)
 */
@Serializable
data class FoldingRange(
    /**
     * The zero-based start line of the range to fold. The folded area starts
     * after the line's last character. To be valid, the end must be zero or
     * larger and smaller than the number of lines in the document.
     */
    val startLine: UInt,

    /**
     * The zero-based character offset from where the folded range starts. If
     * not defined, defaults to the length of the start line.
     */
    val startCharacter: UInt?,

    /**
     * The zero-based end line of the range to fold. The folded area ends with
     * the line's last character. To be valid, the end must be zero or larger
     * and smaller than the number of lines in the document.
     */
    val endLine: UInt,

    /**
     * The zero-based character offset before the folded range ends. If not
     * defined, defaults to the length of the end line.
     */
    val endCharacter: UInt?,

    /**
     * Describes the kind of the folding range such as `comment` or `region`.
     * The kind is used to categorize folding ranges and used by commands like
     * 'Fold all comments'. See [FoldingRangeKind] for an
     * enumeration of standardized kinds.
     *
     * @see FoldingRangeKind
     */
    val kind: FoldingRangeKind?,

    /**
     * The text that the client should show when the specified range is
     * collapsed. If not defined or not supported by the client, a default
     * will be chosen by the client.
     *
     * @since 3.17.0 - proposed
     */
    val collapsedText: String?
)
