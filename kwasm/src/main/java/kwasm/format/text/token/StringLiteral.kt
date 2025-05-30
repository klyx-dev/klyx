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
import kwasm.format.shiftColumnBy
import kwasm.format.text.token.util.STRINGELEM_PATTERN
import kwasm.format.text.token.util.StringChar
import kwasm.format.text.token.util.TokenMatchResult
import kwasm.format.text.token.util.parseStringElem

/**
 * A [String] literal.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#strings):
 *
 * ```
 *   string ::= ‘"’ (b*:stringelem)* ‘"’ => concat((b*)*) (if |concat((b*)*)| < 2^32)
 * ```
 *
 * The JVM will crash if the string is 4 billion characters, so no need to perform the check.
 */
open class StringLiteral(
    private val sequence: CharSequence,
    override val context: ParseContext? = null
) : Token {
    val value: String by lazy {
        val builder = StringBuilder()

        if (sequence.first() != '"') {
            throw ParseException("Expecting opening \" for StringLiteral", context)
        }
        if (sequence.last() != '"') {
            throw ParseException("Expecting closing \" for StringLiteral", context)
        }

        val internalSequence = sequence.subSequence(1, sequence.length - 1).codePoints().toArray()

        val stringElem = StringChar()
        var i = 0
        while (i < internalSequence.size) {
            internalSequence.parseStringElem(i, stringElem, context.shiftColumnBy(i + 1))
            builder.appendCodePoint(stringElem.value)
            i += stringElem.sequenceLength
        }

        builder.toString()
    }

    override fun equals(other: Any?): Boolean = other is StringLiteral && other.value == value

    override fun hashCode(): Int = value.hashCode()

    companion object {
        internal val PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex = "(\"($STRINGELEM_PATTERN)*\")".toRegex()
        }
    }
}

fun RawToken.findStringLiteral(): TokenMatchResult? {
    val match = StringLiteral.PATTERN.get().findAll(sequence)
        .maxByOrNull { it.value.length } ?: return null
    return TokenMatchResult(match.range.first, match.value)
}

fun RawToken.isStringLiteral(): Boolean = StringLiteral.PATTERN.get().matchEntire(sequence) != null

fun RawToken.toStringLiteral(): StringLiteral = StringLiteral(sequence, context)
