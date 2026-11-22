package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokenModifiers)
 */
@JvmInline
@Serializable
value class SemanticTokenModifiers private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        val Declaration = SemanticTokenModifiers("declaration")
        val Definition = SemanticTokenModifiers("definition")
        val Readonly = SemanticTokenModifiers("readonly")
        val Static = SemanticTokenModifiers("static")
        val Deprecated = SemanticTokenModifiers("deprecated")
        val Abstract = SemanticTokenModifiers("abstract")
        val Async = SemanticTokenModifiers("async")
        val Modification = SemanticTokenModifiers("modification")
        val Documentation = SemanticTokenModifiers("documentation")
        val DefaultLibrary = SemanticTokenModifiers("defaultLibrary")
    }
}
