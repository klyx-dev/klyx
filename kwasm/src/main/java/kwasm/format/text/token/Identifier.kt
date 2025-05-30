/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kwasm.format.text.token

import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.text.token.util.TokenMatchResult
import kwasm.ast.Identifier as AstIdentifier

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#text-id):
 *
 * Indices can be given in both numeric and symbolic form. Symbolic identifiers that stand in lieu
 * of indices start with `$`, followed by any sequence of printable ASCII characters that does not
 * contain a space, quotation mark, comma, semicolon, or bracket.
 *
 * ```
 *   id     ::= '$' idchar+
 *   idchar ::= '0', '1', ..., '9'
 *              'A', 'B', ..., 'Z'
 *              'a', 'b', ..., 'z'
 *              '!', '#', '$', '%', '&', ''', '*', '+', '-', '.', '/'
 *              ':', '<', '=', '>', '?', '@', '\', '^', '_', '`', '|', '~'
 * ```
 */
@Suppress("EqualsOrHashCode")
class Identifier(
    private val sequence: CharSequence,
    override val context: ParseContext? = null
) : Token {
    val value: String by lazy {
        if (sequence.first() != '$') throw ParseException("Identifier must begin with $", context)

        if (!PATTERN.get().matches(sequence)) {
            throw ParseException("Invalid identifier: $sequence", context)
        }

        "$sequence"
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is kwasm.format.text.token.Identifier) return false
        return other.sequence == sequence
    }

    /** Gets an instance of a [kwasm.ast.Identifier] based on the [value]. */
    inline fun <reified IdentifierType : AstIdentifier> getAstValue(): IdentifierType =
        when (IdentifierType::class) {
            AstIdentifier.Type::class -> AstIdentifier.Type(stringRepr = value)
            AstIdentifier.Function::class -> AstIdentifier.Function(stringRepr = value)
            AstIdentifier.Global::class -> AstIdentifier.Global(stringRepr = value)
            AstIdentifier.Label::class -> AstIdentifier.Label(stringRepr = value)
            AstIdentifier.Local::class -> AstIdentifier.Local(stringRepr = value)
            AstIdentifier.Memory::class -> AstIdentifier.Memory(stringRepr = value)
            AstIdentifier.Table::class -> AstIdentifier.Table(stringRepr = value)
            else -> throw ParseException(
                "Unsupported AST Identifier type: ${IdentifierType::class.java}",
                context
            )
        } as IdentifierType

    companion object {
        internal const val IDCHAR_REGEX_CLASS = "a-zA-Z0-9!#\\\$%&'*+\\-./:<=>?@\\\\^_`|~"

        internal val PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex = "(\\\$[$IDCHAR_REGEX_CLASS]+)".toRegex()
        }
    }
}

fun RawToken.findIdentifier(): TokenMatchResult? {
    val match = Identifier.PATTERN.get()
        .findAll(sequence).maxByOrNull { it.value.length } ?: return null
    return TokenMatchResult(match.range.first, match.value)
}

fun RawToken.isIdentifier(): Boolean =
    Identifier.PATTERN.get().matchEntire(sequence) != null

fun RawToken.toIdentifier(): Identifier = Identifier(sequence, context)
