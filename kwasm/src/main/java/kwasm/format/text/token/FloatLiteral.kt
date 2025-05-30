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
import kwasm.format.text.token.util.CanonincalNaN
import kwasm.format.text.token.util.Frac
import kwasm.format.text.token.util.Num
import kwasm.format.text.token.util.NumberConstants
import kwasm.format.text.token.util.TokenMatchResult
import kwasm.format.text.token.util.significand
import kotlin.NumberFormatException
import kotlin.math.pow

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#floating-point).
 *
 * ```
 *   float    ::= p:num                                                 => p
 *                p:num '.' q:frac                                      => p + q
 *                p:num ('E' | 'e') s:sign e:num                        => p * 10^(s * e)
 *                p:num '.' q:frac ('E' | 'e') s:sign e:num             => (p + q) * 10^(s * e)
 *   hexfloat ::= '0x' p:hexnum                                         => p
 *                '0x' p:hexnum '.' q:hexfrac                           => p + q
 *                '0x' p:hexnum  ('P' | 'p') s:sign e:num               => p * 2^(s * e)
 *                '0x' p:hexnum '.' q:hexfrac ('P' | 'p') s:sign e:num  => (p + q) * 2^(s * e)
 * ```
 *
 * And:
 *
 * ```
 *   fN       ::= s:sign z:fNMag                                      => s * z
 *   fNMag    ::= z:float                                             => float_N(z) (if != inf)
 *                z:hexfloat                                          => float_N(z) (if != inf)
 *                'inf'                                               => inf
 *                'nan'                                               => NaN
 *                'nan:0x' n:hexnum                                   => NaN (if 1 <= n < 2^(sig(N))
 * ```
 */
@OptIn(ExperimentalUnsignedTypes::class)
class FloatLiteral(
    private val sequence: CharSequence,
    magnitude: Int = NumberConstants.DEFAULT_FLOAT_MAGNITUDE,
    override val context: ParseContext? = null
) : Token {
    var magnitude: Int = magnitude
        set(value) {
            check(value == 32 || value == 64) { "Magnitude must be either 32 or 64" }
            isInitialized = false
            field = value
        }

    private var isInitialized = false
    private var cachedValue: Number = 0.0

    val value: Number
        get() {
            if (isInitialized) return cachedValue
            return when {
                sequence == INFINITY_LITERAL ->
                    if (magnitude == 32) Float.POSITIVE_INFINITY
                    else Double.POSITIVE_INFINITY
                sequence == "-$INFINITY_LITERAL" ->
                    if (magnitude == 32) Float.NEGATIVE_INFINITY
                    else Double.NEGATIVE_INFINITY
                sequence == NAN_LITERAL ||
                    sequence == CANONICAL_NAN_LITERAL ||
                    sequence == ARITHMETIC_NAN_LITERAL -> {
                    if (magnitude == 32) NAN_32_VALUE else NAN_64_VALUE
                }
                sequence == "-$NAN_LITERAL" -> if (magnitude == 32) -NAN_32_VALUE else -NAN_64_VALUE
                sequence.startsWith("-$HEXNAN_LITERAL_PREFIX") -> {
                    val posValue =
                        FloatLiteral(
                            sequence.subSequence(1, sequence.length),
                            magnitude,
                            context
                        ).value
                    if (magnitude == 32) {
                        -posValue.toFloat()
                    } else {
                        -posValue.toDouble()
                    }
                }
                sequence.startsWith(HEXNAN_LITERAL_PREFIX) -> {
                    val n = java.lang.Long.parseUnsignedLong(
                        sequence.substring(
                            HEXNAN_LITERAL_PREFIX.length,
                            sequence.length
                        ).replace("_", ""),
                        16
                    )

                    if (n < 1L || n >= 2.0.pow(magnitude.significand(context))) {
                        throw ParseException(
                            errorMsg = "Illegal hex NaN value (constant out of range).",
                            parseContext = context
                        )
                    }

                    if (magnitude == 32) {
                        Float.fromBits(bits = 0x7F800000 or n.toInt())
                    } else {
                        Double.fromBits(bits = 0x7FF0000000000000L or n)
                    }
                }
                else -> determineFloatValue(sequence, context)
            }.also {
                cachedValue = it
                isInitialized = true
            }
        }

    fun isNaN(): Boolean = when (magnitude) {
        32 -> value.toFloat().isNaN() || value == CanonincalNaN(32).value
        64 -> value.toDouble().isNaN() || value == CanonincalNaN(64).value.toDouble()
        else -> throw ParseException("Illegal magnitude for NaN-checking")
    }

    fun isInfinite(): Boolean = when (magnitude) {
        32 -> value == Float.NEGATIVE_INFINITY || value == Float.POSITIVE_INFINITY
        64 -> value == Double.NEGATIVE_INFINITY || value == Double.POSITIVE_INFINITY
        else -> throw ParseException("Illegal magnitude for infinity-checking")
    }

    private fun determineFloatValue(
        sequence: CharSequence,
        context: ParseContext? = this.context
    ): Number {
        try {
            return if (sequence.indexOf("0x") > -1) {
                determineHexFloatValue(sequence, context)
            } else {
                determineDecimalFloatValue(sequence, context)
            }
        } catch (e: NumberFormatException) {
            throw ParseException("Invalid f$magnitude format (unknown operator)", context, e)
        }
    }

    /**
     * ```
     *   float    ::= p:num                                                 => p
     *                p:num '.' q:frac                                      => p + q
     *                p:num ('E' | 'e') s:sign e:num                        => p * 10^(s * e)
     *                p:num '.' q:frac ('E' | 'e') s:sign e:num             => (p + q) * 10^(s * e)
     * ```
     */
    private fun determineDecimalFloatValue(
        sequence: CharSequence,
        context: ParseContext?
    ): Number {
        val dotIndex = sequence.indexOf('.')
        val eIndex = sequence.indexOf('e', ignoreCase = true)

        if (dotIndex == 0) throw ParseException("Invalid placement for decimal in float", context)
        if (eIndex == sequence.length - 1) {
            throw ParseException("Invalid exponent for float (no contents)", context)
        }

        var strToUse = sequence.toString().replace("_", "")
        strToUse = if (dotIndex == strToUse.length - 1) {
            strToUse + "0"
        } else strToUse
        return parseValue(strToUse, context)
    }

    /**
     * ```
     *   hexfloat ::= '0x' p:hexnum                                         => p
     *                '0x' p:hexnum '.' q:hexfrac                           => p + q
     *                '0x' p:hexnum ('P' | 'p') s:sign e:num                => p * 2^(s * e)
     *                '0x' p:hexnum '.' q:hexfrac ('P' | 'p') s:sign e:num  => (p + q) * 2^(s * e)
     * ```
     */
    private fun determineHexFloatValue(
        sequence: CharSequence,
        context: ParseContext?
    ): Number {
        val dotIndex = sequence.indexOf('.')
        val eIndex = sequence.indexOf('p', ignoreCase = true)

        if (dotIndex == 0) throw ParseException("Invalid placement for decimal in float", context)
        if (eIndex == sequence.length - 1) {
            throw ParseException("Invalid exponent for float (no contents)", context)
        }
        var strToUse = sequence.toString()
        strToUse = if (dotIndex == sequence.length - 1) {
            sequence.toString() + "0p0"
        } else sequence.toString()

        strToUse = strToUse.replace("_", "")
        strToUse = if (strToUse.contains('p', ignoreCase = true)) {
            strToUse
        } else {
            strToUse + "p0"
        }
        return parseValue(strToUse, context)
    }

    private fun parseValue(str: String, context: ParseContext?): Number {
        return if (magnitude == 32) {
            val floatValue = java.lang.Float.parseFloat(str)
            if (floatValue.isNaN() || floatValue.isInfinite()) {
                throw ParseException("Illegal f32 (constant out of range)", context)
            }
            floatValue
        } else {
            val result = java.lang.Double.parseDouble(str)
            if (result.isNaN() || result.isInfinite()) {
                throw ParseException("Illegal f32 (constant out of range)", context)
            }
            result
        }
    }

    companion object {
        private val DECIMAL_FLOAT_PATTERN =
            "((${Num.DECIMAL_PATTERN})(\\.(${Frac.DECIMAL_PATTERN})?)?([Ee][+-]?${Num.DECIMAL_PATTERN})?)"
        private val HEX_FLOAT_PATTERN =
            "(0x(${Num.HEX_PATTERN})(\\.(${Frac.HEX_PATTERN})?)?([Pp][+-]?${Num.HEX_PATTERN})?)"

        private val FLOAT_PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex =
                "($HEX_FLOAT_PATTERN|$DECIMAL_FLOAT_PATTERN)".toRegex()
        }

        internal val PATTERN = object : ThreadLocal<Regex>() {
            override fun initialValue(): Regex =
                "([+-]?((${FLOAT_PATTERN.get()})|(inf|nan(:0x${Num.HEX_PATTERN})?|nan:canonical|nan:arithmetic)))".toRegex()
        }

        private const val INFINITY_LITERAL = "inf"
        private const val NAN_LITERAL = "nan"
        private const val CANONICAL_NAN_LITERAL = "nan:canonical"
        private const val ARITHMETIC_NAN_LITERAL = "nan:arithmetic"
        private const val HEXNAN_LITERAL_PREFIX = "nan:0x"
        private const val NAN_64_VALUE = Double.NaN
        private const val NAN_32_VALUE = Float.NaN

        private fun assertNoHexChars(context: ParseContext?, vararg components: Any) {
            val foundHexChars = components.any {
                (it as? Num)?.foundHexChars == true || (it as? Frac)?.foundHexChars == true
            }
            if (foundHexChars) {
                throw ParseException("Found illegal hex digits in expected base-10 float", context)
            }
        }
    }
}

fun RawToken.findFloatLiteral(): TokenMatchResult? {
    val match =
        FloatLiteral.PATTERN.get().findAll(sequence).maxByOrNull { it.value.length } ?: return null
    return TokenMatchResult(match.range.first, match.value)
}

fun RawToken.isFloatLiteral(): Boolean = FloatLiteral.PATTERN.get().matchEntire(sequence) != null

fun RawToken.toFloatLiteral(): FloatLiteral = FloatLiteral(sequence, context = context)
