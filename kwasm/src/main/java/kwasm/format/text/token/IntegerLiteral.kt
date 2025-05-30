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
import kwasm.format.text.token.util.Num
import kwasm.format.text.token.util.TokenMatchResult
import kwasm.format.text.token.util.parseLongSign
import java.lang.NumberFormatException
import kotlin.math.pow

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#integers):
 *
 * The allowed syntax for integer literals depends on size and signedness. Moreover, their value
 * must lie within the range of the respective type
 *
 * ```
 *   sign       ::= empty => +
 *                  '+' => +
 *                  '-' => -
 *   uN         ::= n:num         => n (if n < 2^N)
 *                  '0x' n:hexnum => n (if n < 2^N)
 *   sN         ::= plusminus:sign n:num => plusminus * n (if -2^(N-1) <= plusminus n < 2^(N-1))
 *                  plusminus:sign n:hexnum => plusminus * n (same conditions as above)
 * ```.
 *
 * Uninterpreted integers can be written as either signed or unsigned, and are normalized to
 * unsigned in the abstract syntax.
 *
 * ```
 *   iN         ::= n:uN => n
 *                  i:sN => n (if i = signed(n))
 * ```
 */
@OptIn(ExperimentalUnsignedTypes::class)
sealed class IntegerLiteral<Type : Any>(
    val sequence: CharSequence,
    magnitude: Int = 64,
    override val context: ParseContext? = null
) : Token {
    private lateinit var calculatedValue: Type
    private var calculated = false

    val value: Type
        get() {
            if (calculated) return calculatedValue
            val res = try {
                parseValue()
            } catch (e: NumberFormatException) {
                throw ParseException(
                    errorMsg = "Illegal value: $sequence (i$magnitude constant out of range)",
                    parseContext = context,
                    origin = e
                )
            }
            if (!checkMagnitude(res, this.magnitude)) {
                throw ParseException(
                    "Illegal value: $res, for expected magnitude: " +
                        "${this.magnitude}: (i${this.magnitude} constant constant out of range)",
                    context
                )
            }
            return res.also {
                calculatedValue = res
                calculated = true
            }
        }

    var magnitude: Int = magnitude
        set(value) {
            check(value >= 0) { "Negative magnitudes are not allowed" }
            check(value <= 64) { "Magnitudes above 64 are not allowed" }
            field = value
            calculated = false
        }

    protected abstract fun parseValue(): Type
    protected abstract fun checkMagnitude(value: Type, magnitude: Int): Boolean
    abstract fun toUnsigned(): Unsigned
    abstract fun toSigned(): Signed

    class Unsigned(
        sequence: CharSequence,
        magnitude: Int = 64,
        context: ParseContext? = null
    ) : IntegerLiteral<ULong>(sequence, magnitude, context) {
        private val bounds = 0uL..(2.0.pow(magnitude) - 1).toULong()

        override fun parseValue(): ULong {
            var strToUse = sequence.toString().replace("_", "").lowercase()
            var radix = 10
            if (strToUse.startsWith("0x")) {
                strToUse = strToUse.substring(2)
                radix = 16
            }
            val result = if (magnitude <= 32) {
                Integer.parseUnsignedInt(strToUse, radix).toUInt().toULong()
            } else {
                java.lang.Long.parseUnsignedLong(strToUse, radix).toULong()
            }
            return result.takeIf { it in bounds }
                ?: throw ParseException("Integer constant out of range", context)
        }

        override fun checkMagnitude(value: ULong, magnitude: Int): Boolean = when (magnitude) {
            32 -> value <= UInt.MAX_VALUE
            64 -> value <= ULong.MAX_VALUE
            else -> value.toDouble() < 2.0.pow(magnitude)
        }

        override fun toUnsigned(): Unsigned = this

        override fun toSigned(): Signed = Signed(sequence, magnitude, context)
    }

    class Signed(
        sequence: CharSequence,
        magnitude: Int = 64,
        context: ParseContext? = null
    ) : IntegerLiteral<Long>(sequence, magnitude, context) {
        override fun parseValue(): Long {
            var strToUse = sequence.toString().replace("_", "").lowercase()
            var radix = 10
            if (strToUse.startsWith("0x")) {
                strToUse = strToUse.substring(2)
                radix = 16
            } else if (strToUse.startsWith("-0x")) {
                strToUse = "-" + strToUse.substring(3)
                radix = 16
            } else if (strToUse.startsWith("+0x")) {
                strToUse = strToUse.substring(3)
                radix = 16
            }
            return if (magnitude <= 32) {
                if (strToUse.startsWith("-")) {
                    java.lang.Long.parseLong(strToUse, radix)
                } else {
                    Integer.parseUnsignedInt(strToUse, radix).toLong()
                }
            } else if (strToUse.startsWith("-")) {
                java.lang.Long.parseLong(strToUse, radix)
            } else {
                java.lang.Long.parseUnsignedLong(strToUse, radix)
            }
        }

        override fun checkMagnitude(value: Long, magnitude: Int): Boolean {
            val (_, sign) = sequence.parseLongSign()
            if (magnitude == 32) {
                return value.toUInt().toInt() in Int.MIN_VALUE..Int.MAX_VALUE &&
                    (
                        (sign == 1L && value <= UInt.MAX_VALUE.toLong()) ||
                            (sign == -1L && value >= Int.MIN_VALUE.toLong())
                        )
            }
            if (magnitude == 64) return value in Long.MIN_VALUE..Long.MAX_VALUE
            val doubleValue = value.toDouble()
            val extent = 2.0.pow(magnitude - 1)
            return -extent <= doubleValue && doubleValue < extent
        }

        override fun toUnsigned(): Unsigned = Unsigned(sequence, magnitude, context)

        override fun toSigned(): Signed = this
    }

    companion object {
        private const val DECIMAL_PATTERN = "(${Num.DECIMAL_PATTERN})"
        private const val HEX_PATTERN = "(0x(${Num.HEX_PATTERN}))"

        internal val PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex = "([-+]?($HEX_PATTERN|$DECIMAL_PATTERN))".toRegex()
        }
    }
}

fun RawToken.findIntegerLiteral(): TokenMatchResult? {
    val match = IntegerLiteral.PATTERN.get().findAll(sequence)
        .maxByOrNull { it.value.length } ?: return null
    return TokenMatchResult(match.range.first, match.value)
}

fun RawToken.isIntegerLiteral(): Boolean =
    IntegerLiteral.PATTERN.get().matchEntire(sequence) != null

fun RawToken.toIntegerLiteral(): IntegerLiteral<*> =
    if ('-' in sequence || '+' in sequence) {
        IntegerLiteral.Signed(sequence, context = context)
    } else {
        IntegerLiteral.Unsigned(sequence, context = context)
    }
