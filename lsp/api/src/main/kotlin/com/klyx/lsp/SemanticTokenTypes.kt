package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#semanticTokenTypes)
 */
@Serializable
@JvmInline
value class SemanticTokenTypes private constructor(private val value: String) {
    override fun toString() = value

    companion object {
        val Namespace = SemanticTokenTypes("namespace")

        /**
         * Represents a generic type. Acts as a fallback for types which
         * can't be mapped to a specific type like class or enum.
         */
        val Type = SemanticTokenTypes("type")
        val Class = SemanticTokenTypes("class")
        val Enum = SemanticTokenTypes("enum")
        val Interface = SemanticTokenTypes("interface")
        val Struct = SemanticTokenTypes("struct")
        val TypeParameter = SemanticTokenTypes("typeParameter")
        val Parameter = SemanticTokenTypes("parameter")
        val Variable = SemanticTokenTypes("variable")
        val Property = SemanticTokenTypes("property")
        val EnumMember = SemanticTokenTypes("enumMember")
        val Event = SemanticTokenTypes("event")
        val Function = SemanticTokenTypes("function")
        val Method = SemanticTokenTypes("method")
        val Macro = SemanticTokenTypes("macro")
        val Keyword = SemanticTokenTypes("keyword")
        val Modifier = SemanticTokenTypes("modifier")
        val Comment = SemanticTokenTypes("comment")
        val String = SemanticTokenTypes("string")
        val Number = SemanticTokenTypes("number")
        val Regexp = SemanticTokenTypes("regexp")
        val Operator = SemanticTokenTypes("operator")

        /**
         * @since 3.17.0
         */
        val Decorator = SemanticTokenTypes("decorator")

        /**
         * @since 3.18.0
         */
        val Label = SemanticTokenTypes("label")
    }
}
