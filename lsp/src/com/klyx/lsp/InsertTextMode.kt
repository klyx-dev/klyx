package com.klyx.lsp

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * How whitespace and indentation is handled during completion
 * item insertion.
 *
 * [LSP Specification](https://microsoft.github.io/language-server-protocol/specifications/lsp/3.18/specification/#insertTextMode)
 *
 * @since 3.16.0
 */
@JvmInline
@Serializable
value class InsertTextMode private constructor(private val value: Int) {
    companion object {
        /**
         * The insertion or replace strings are taken as-is. If the
         * value is multiline, the lines below the cursor will be
         * inserted using the indentation defined in the string value.
         * The client will not apply any kind of adjustments to the
         * string.
         */
        val asIs = InsertTextMode(1)

        /**
         * The editor adjusts leading whitespace of new lines so that
         * they match the indentation up to the cursor of the line for
         * which the item is accepted.
         *
         * Consider a line like this: <2tabs><cursor><3tabs>foo. Accepting a
         * multi line completion item is indented using 2 tabs and all
         * following lines inserted will be indented using 2 tabs as well.
         */
        val adjustIndentation = InsertTextMode(2)
    }
}
