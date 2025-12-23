package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * The type is a string since the value set is extensible
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#foldingRangeKind)
 */
@JvmInline
@Serializable
value class FoldingRangeKind(private val value: String) {
    override fun toString() = value

    companion object {
        /**
         * Folding range for a comment
         */
        val Comment = FoldingRangeKind("comment")

        /**
         * Folding range for imports or includes
         */
        val Imports = FoldingRangeKind("imports")

        /**
         * Folding range for a region (e.g. `#region`)
         */
        val Region = FoldingRangeKind("region")
    }
}

