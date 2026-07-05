package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A type indicating how positions are encoded,
 * specifically what column offsets mean.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#positionEncodingKind)
 *
 * @since 3.17.0
 */
@JvmInline
@Serializable
value class PositionEncodingKind private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        /**
         * Character offsets count UTF-8 code units (i.e. bytes).
         */
        val UTF8 = PositionEncodingKind("utf-8")

        /**
         * Character offsets count UTF-16 code units.
         *
         * This is the default and must always be supported
         * by servers.
         */
        val UTF16 = PositionEncodingKind("utf-16")

        /**
         * Character offsets count UTF-32 code units.
         *
         * Implementation note: these are the same as Unicode code points,
         * so this `PositionEncodingKind` may also be used for an
         * encoding-agnostic representation of character offsets.
         */
        val UTF32 = PositionEncodingKind("utf-32")
    }
}

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#position)
 */
@Serializable
data class Position(
    /**
     * Line position in a document (zero-based).
     */
    val line: UInt,

    /**
     * Character offset on a line in a document (zero-based). The meaning of this
     * offset is determined by the negotiated [PositionEncodingKind].
     *
     * If the character value is greater than the line length it defaults back
     * to the line length.
     */
    val character: UInt,
)

/**
 * Creates a [Position] instance.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#position)
 *
 * @param line Line position in a document (zero-based).
 * @param character Character offset on a line in a document (zero-based).
 *                  The meaning of this offset is determined by the negotiated [PositionEncodingKind].
 *                  If the character value is greater than the line length it defaults back to the line length.
 */
fun Position(line: Int, character: Int) = Position(line.toUInt(), character.toUInt())
