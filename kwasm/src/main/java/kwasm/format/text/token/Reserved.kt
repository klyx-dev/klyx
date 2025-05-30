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

/**
 * Represents a reserved word.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/lexical.html#tokens):
 *
 * ```
 *   reserved ::= idchar+
 * ```
 */
data class Reserved(
    private val sequence: CharSequence,
    override val context: ParseContext? = null
) : Token {
    val value: String by lazy {
        if (PATTERN.get().matchEntire(sequence) == null) {
            throw ParseException("Expected reserved word, but value was illegal.", context)
        }

        sequence.toString()
    }

    companion object {
        internal val PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex = "([${Identifier.IDCHAR_REGEX_CLASS}]+)".toRegex()
        }
    }
}

fun RawToken.findReserved(): TokenMatchResult? {
    val match = Reserved.PATTERN.get().findAll(sequence)
        .maxByOrNull { it.value.length } ?: return null
    if (match.value == "inf" || match.value == "nan") return null
    return TokenMatchResult(match.range.first, match.value)
}

fun RawToken.isReserved(): Boolean = Reserved.PATTERN.get().matchEntire(sequence) != null

fun RawToken.toReserved(): Reserved = Reserved(sequence, context)
