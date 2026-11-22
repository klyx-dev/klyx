package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * A string value used as a snippet is a template which allows to insert text
 * and to control the editor cursor when insertion happens.
 *
 * A snippet can define tab stops and placeholders with `$1`, `$2`
 * and `${3:foo}`. `$0` defines the final tab stop, it defaults to
 * the end of the snippet. Variables are defined with `$name` and
 * `${name:default value}`.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#stringValue)
 *
 * @since 3.18.0
 */
@Serializable
data class StringValue(
    /**
     * The snippet string.
     */
    val value: String,

    /**
     * The kind of string value.
     */
    val kind: StringValueKind = StringValueKind.Snippet
)

@Serializable
@JvmInline
value class StringValueKind private constructor(private val kind: String) {
    override fun toString() = kind

    companion object {
        val Snippet = StringValueKind("snippet")
    }
}
