package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Symbol tags are extra annotations that tweak the rendering of a symbol.
 *
 * @since 3.16
 */
@JvmInline
@Serializable
value class SymbolTag private constructor(private val value: Int) {
    companion object {
        /**
         * Render a symbol as obsolete, usually using a strike-out.
         */
        val Deprecated = SymbolTag(1)
    }
}
