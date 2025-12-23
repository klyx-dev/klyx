package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Completion item tags are extra annotations that tweak the rendering of a
 * completion item.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#completionItemTag)
 *
 * @since 3.15.0
 */
@JvmInline
@Serializable
value class CompletionItemTag private constructor(private val value: Int) {
    companion object {
        /**
         * Render a completion as obsolete, usually using a strike-out.
         */
        val Deprecated = CompletionItemTag(1)
    }
}
