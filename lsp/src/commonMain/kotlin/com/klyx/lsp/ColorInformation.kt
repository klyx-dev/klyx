package com.klyx.lsp

import kotlinx.serialization.Serializable

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#colorInformation)
 */
@Serializable
data class ColorInformation(
    /**
     * The range in the document where this color appears.
     */
    val range: Range,

    /**
     * The actual color value for this color range.
     */
    val color: Color
)

/**
 * Represents a color in RGBA space.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#color)
 */
@Serializable
data class Color(
    /**
     * The red component of this color in the range [0-1].
     */
    val red: Float,

    /**
     * The green component of this color in the range [0-1].
     */
    val green: Float,

    /**
     * The blue component of this color in the range [0-1].
     */
    val blue: Float,

    /**
     * The alpha component of this color in the range [0-1].
     */
    val alpha: Float
)
