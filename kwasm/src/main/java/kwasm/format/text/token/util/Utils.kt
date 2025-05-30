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

package kwasm.format.text.token.util

import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.shiftColumnBy
import kwasm.format.text.token.util.StringConstants.BACKSLASH
import kwasm.format.text.token.util.StringConstants.DELETE
import kwasm.format.text.token.util.StringConstants.DQUOTE
import kwasm.format.text.token.util.StringConstants.N
import kwasm.format.text.token.util.StringConstants.NEWLINE
import kwasm.format.text.token.util.StringConstants.QUOTE
import kwasm.format.text.token.util.StringConstants.R
import kwasm.format.text.token.util.StringConstants.RETURN
import kwasm.format.text.token.util.StringConstants.SPACE
import kwasm.format.text.token.util.StringConstants.T
import kwasm.format.text.token.util.StringConstants.TAB
import kwasm.format.text.token.util.StringConstants.UNICODE_PATTERN
import kotlin.math.pow

/**
 * Pattern to check for valid StringChar elements.
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#text-string):
 *
 * ```
 *   stringchar ::= c:char                  => c (if c ≥ U+20 ∧ c ≠ U+7F ∧ c ≠ ‘"’ ∧ c ≠ ‘∖’)
 *                  '∖t'                    => U+09
 *                  '∖n'                    => U+0A
 *                  '∖r'                    => U+0D
 *                  '∖"'                    => U+22
 *                  '∖''                    => U+27
 *                  '∖\'                    => U+5C
 *                  '∖u{' n:hexnum '}'      => U+(n) (if n < 0xD800 ∨ 0xE000 ≤ n < 0x110000)
 * ```
 */
const val STRINGCHAR_PATTERN = "([^\\u007F\"\\\\]|(\\\\(t|n|r|\"|\'|'|u\\{([0-9a-fA-F]+)\\})))"

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#text-string):
 *
 * ```
 *   stringelem ::= s:stringchar                => s as StringChar
 *                  '∖' n:hexdigit m:hexdigit   => StringChar(16 * n + m, 3)
 * ```
 */
const val STRINGELEM_PATTERN = "(($STRINGCHAR_PATTERN)|(\\\\[0-9a-fA-F]{2}))"

/**
 * Parses a sign (`+` or `-`) from the beginning of the receiving [CharSequence], and returns the
 * sign value and intended offset for parsing the remainder of the value.
 */
fun CharSequence.parseLongSign(): Pair<Int, Long> = when (this[0]) {
    '-' -> NumberConstants.negativeLongWithOffset
    '+' -> NumberConstants.positiveLongWithOffset
    else -> NumberConstants.positiveLong
}

/** Parses a digit (as a [Byte]) from the receiving [CharSequence] at the given [index]. */
fun CharSequence.parseDigit(index: Int, context: ParseContext? = null): Byte =
    this[index].parseDigit(context)
fun IntArray.parseDigit(index: Int, context: ParseContext? = null): Byte =
    this[index].toChar().parseDigit(context)
fun Char.parseDigit(context: ParseContext? = null): Byte = when (this) {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> (toInt() - 48).toByte()
    'a', 'b', 'c', 'd', 'e', 'f' -> (toInt() - 97 + 10).toByte()
    'A', 'B', 'C', 'D', 'E', 'F' -> (toInt() - 65 + 10).toByte()
    '_' -> NumberConstants.UNDERSCORE
    else -> throw ParseException("Illegal char '$this' in expected number.", context)
}

/**
 * Parses a `stringelem` from the receiving [CharSequence] at the given [index] as a [StringChar].
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#text-string):
 *
 * ```
 *   stringelem ::= s:stringchar                => s as StringChar
 *                  '∖' n:hexdigit m:hexdigit   => StringChar(16 * n + m, 3)
 * ```
 */
fun CharSequence.parseStringElem(
    index: Int,
    inoutVal: StringChar = StringChar(),
    context: ParseContext? = null
): StringChar = codePoints().toArray().parseStringElem(index, inoutVal, context)
fun IntArray.parseStringElem(
    index: Int,
    inoutVal: StringChar = StringChar(),
    context: ParseContext? = null
): StringChar {
    return if (
        this[index] == BACKSLASH &&
        index <= this.size - 3 &&
        this[index + 1].toChar().isHexDigit() &&
        this[index + 2].toChar().isHexDigit()
    ) {
        inoutVal.sequenceLength = 3
        inoutVal.value = 16 * parseDigit(index + 1, context) + parseDigit(index + 2, context)
        inoutVal
    } else {
        parseStringChar(index, inoutVal, context)
    }
}

private fun Char.isHexDigit(): Boolean = when (this) {
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> true
    'a', 'b', 'c', 'd', 'e', 'f' -> true
    'A', 'B', 'C', 'D', 'E', 'F' -> true
    else -> false
}

/**
 * Parses a [StringChar] from the receiving [CharSequence] at the given [index].
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#text-string):
 *
 * ```
 *   stringchar ::= c:char                  => c (if c ≥ U+20 ∧ c ≠ U+7F ∧ c ≠ ‘"’ ∧ c ≠ ‘∖’)
 *                  '∖t'                    => U+09
 *                  '∖n'                    => U+0A
 *                  '∖r'                    => U+0D
 *                  '∖"'                    => U+22
 *                  '∖''                    => U+27
 *                  '∖\'                    => U+5C
 *                  '∖u{' n:hexnum '}'      => U+(n) (if n < 0xD800 ∨ 0xE000 ≤ n < 0x110000)
 * ```
 */
@OptIn(ExperimentalUnsignedTypes::class)
fun CharSequence.parseStringChar(
    index: Int,
    inoutVal: StringChar = StringChar(),
    context: ParseContext? = null
): StringChar = codePoints().toArray().parseStringChar(index, inoutVal, context)

@OptIn(ExperimentalUnsignedTypes::class)
fun IntArray.parseStringChar(
    index: Int,
    inoutVal: StringChar = StringChar(),
    context: ParseContext? = null
): StringChar {
    val c = this[index]
    when {
        c == BACKSLASH -> {
            val escaped = this.takeIf { index <= it.size - 2 }?.get(index + 1)
                ?: throw ParseException("Attempting to escape an empty sequence", context)
            val unicodeMatchString = String(this, index, this.size - index)
            val unicodeMatch =
                UNICODE_PATTERN.get()
                    .find(unicodeMatchString)
                    ?.takeIf { it.range.first == 0 }

            inoutVal.sequenceLength = 2
            inoutVal.value = when {
                escaped == T -> TAB
                escaped == N -> NEWLINE
                escaped == R -> RETURN
                escaped == DQUOTE -> DQUOTE
                escaped == QUOTE -> QUOTE
                escaped == BACKSLASH -> BACKSLASH
                unicodeMatch != null -> {
                    // Parse a hex number from the match.
                    val hexNum = unicodeMatch.groups[1]?.value?.let {
                        Num(
                            it,
                            context.shiftColumnBy(unicodeMatch.groups[1]?.range?.first ?: 0)
                        ).apply { forceHex = true }
                    } ?: throw ParseException(
                        "Illegal unicode value: ${unicodeMatch.value}",
                        context
                    )

                    inoutVal.sequenceLength = unicodeMatch.value.length

                    // Check that the value is within the supported range.
                    val unicodeValue = hexNum.value.toInt()
                    if (unicodeValue >= 0xD800 && unicodeValue !in 0xE000 until 0x110000) {
                        throw ParseException("Unicode value out of valid range", context)
                    }
                    unicodeValue
                }
                else -> {
                    val cString = c.toStringAsCodepoint()
                    val escapedString = escaped.toStringAsCodepoint()
                    throw ParseException(
                        "Invalid escape sequence: $cString$escapedString",
                        context
                    )
                }
            }
        }
        c >= SPACE && c != DELETE && c != DQUOTE && c != BACKSLASH -> {
            inoutVal.sequenceLength = 1
            inoutVal.value = c
        }
        else -> throw ParseException("Invalid StringChar: $c (U+$c)", context.shiftColumnBy(index))
    }
    return inoutVal
}

/**
 * Represents a single character as a unicode codepoint, and its original length in a wast file as
 * part of a string literal.
 */
data class StringChar(var value: Int = -1, var sequenceLength: Int = 1) {
    override fun toString(): String = String(codePoints = intArrayOf(value), offset = 0, length = 1)
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/syntax/values.html#aux-significand).
 */
fun Int.significand(context: ParseContext? = null): Int = when (this) {
    32 -> 23
    64 -> 52
    else -> throw ParseException("Illegal significand", context)
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/syntax/values.html#aux-exponent).
 */
fun Int.expon(context: ParseContext? = null): Int = when (this) {
    32 -> 8
    64 -> 11
    else -> throw ParseException("Illegal expon", context)
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/syntax/values.html#aux-canon).
 */
data class CanonincalNaN(val magnitude: Int) {
    val value: Long = magnitude.canon()
}

/**
 * From [the docs](https://webassembly.github.io/spec/core/syntax/values.html#aux-canon).
 */
fun Int.canon(context: ParseContext? = null): Long =
    2.0.pow(this.significand(context) - 1).toLong()

/** Return value of [RawToken]'s `find\[Token]` extension methods. */
data class TokenMatchResult(val index: Int, val sequence: CharSequence)

private fun Int.toStringAsCodepoint(): String = String(intArrayOf(this), 0, 1)

internal object NumberConstants {
    val negativeLongWithOffset = 1 to -1L
    val positiveLongWithOffset = 1 to 1L
    val positiveLong = 0 to 1L

    const val UNDERSCORE = (-1).toByte()

    const val DEFAULT_FLOAT_MAGNITUDE = 64
}

internal object StringConstants {
    const val T = 't'.toInt()
    const val N = 'n'.toInt()
    const val R = 'r'.toInt()
    const val SPACE = '\u0020'.toInt()
    const val DELETE = '\u007F'.toInt()
    const val QUOTE = '\u0027'.toInt()
    const val DQUOTE = '\u0022'.toInt()
    const val BACKSLASH = '\u005C'.toInt()
    const val TAB = '\u0009'.toInt()
    const val NEWLINE = '\u000A'.toInt()
    const val RETURN = '\u000D'.toInt()

    val UNICODE_PATTERN = object : ThreadLocal<Regex>() {
        override fun initialValue(): Regex = "\\\\u\\{([0-9a-fA-F]+)\\}".toRegex()
    }
}
