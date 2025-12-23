package com.klyx.lsp

import com.klyx.lsp.types.OneOf
import kotlinx.serialization.Serializable
import kotlin.contracts.contract

@Serializable
data class CodeBlock(
    val language: String,
    val value: String
)

/**
 * MarkedString can be used to render human readable text. It is either a
 * markdown string or a code-block that provides a language and a code snippet.
 * The language identifier is semantically equal to the optional language
 * identifier in fenced code blocks in GitHub issues.
 *
 * The pair of a language and a value is an equivalent to markdown:
 * ```${language}
 * ${value}
 * ```
 *
 * Note that markdown strings will be sanitized - that means html will be
 * escaped.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#markedString)
 */
@Deprecated("use MarkupContent instead.", ReplaceWith("MarkupContent"))
typealias MarkedString = OneOf<String, CodeBlock>

@Suppress("DEPRECATION")
val MarkedString.codeBlock get() = if (isCodeBlock()) value else null

@Suppress("DEPRECATION")
val MarkedString.markdownString get() = if (isMarkdownString()) value else null

/**
 * Returns true if the [MarkedString] is a [CodeBlock].
 */
@Suppress("DEPRECATION")
fun MarkedString.isCodeBlock(): Boolean {
    contract {
        returns(true) implies (this@isCodeBlock is OneOf.Right)
    }
    return this is OneOf.Right
}

/**
 * Returns true if the [MarkedString] is a [String].
 */
@Suppress("DEPRECATION")
fun MarkedString.isMarkdownString(): Boolean {
    contract {
        returns(true) implies (this@isMarkdownString is OneOf.Left)
    }
    return this is OneOf.Left
}
