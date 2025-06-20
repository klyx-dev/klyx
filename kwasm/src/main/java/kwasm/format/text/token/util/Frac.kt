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

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#floating-point).
 *
 * ```
 *   frac       ::= empty                               => 0
 *                  d:digit q:frac                      => (d + q) / 10
 *                  d:digit '_' p:digit q:frac          => (d + (p + q) / 10) / 10
 *   hexfrac    ::= empty                               => 0
 *                  h:hexdigit q:hexfrac                => (h + q) / 16
 *                  h:hexdigit '_' p:hexdigit q:hexfrac => (h + (p + q) / 16) / 16
 * ```
 */
class Frac(private val sequence: CharSequence, private val context: ParseContext? = null) {
    var forceHex: Boolean = false

    val foundHexChars: Boolean by lazy { digits.any { it >= 10 } }

    val value: Double by lazy {
        if (sequence.isEmpty()) return@lazy 0.0

        val divisor = if (foundHexChars || forceHex) 16.0 else 10.0

        if (digits.size == 1) return@lazy digits[0] / divisor

        if (digits[1] != NumberConstants.UNDERSCORE) {
            // d:digit q:frac
            val q = Frac(
                sequence.subSequence(1, sequence.length),
                context.shiftColumnBy(1)
            )
            q.forceHex = forceHex
            return@lazy (digits[0] + q.value) / divisor
        }

        if (digits.size < 3) throw ParseException("Illegal format for fraction", context)

        val d = digits[0].toDouble()
        val p = digits[2].toDouble()
        val q = Frac(
            if (sequence.length > 3) sequence.subSequence(3, sequence.length) else "",
            context.shiftColumnBy(3)
        )
        q.forceHex = forceHex

        (d + (p + q.value) / divisor) / divisor
    }

    private val digits: ByteArray by lazy {
        val value = ByteArray(sequence.length)
        repeat(sequence.length) { index -> value[index] = sequence.parseDigit(index, context) }
        value
    }

    companion object {
        const val DECIMAL_PATTERN = "(${Num.DECIMAL_PATTERN}|^$)"
        const val HEX_PATTERN = "(${Num.HEX_PATTERN}|^$)"
    }
}
