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

/**
 * From [the docs](https://webassembly.github.io/spec/core/text/values.html#integers).
 *
 * All integers can be written in either decimal or hexadecimal notation. In both cases, digits can
 * optionally be separated by underscores.
 *
 * ```
 *   digit      ::= '0' => 0, '1' => 1, ... '9' => 9
 *   hexdigit   ::= d:digit => d
 *                  'A' => 10, 'B' => 11, ... 'F' => 15
 *                  'a' => 10, 'b' => 11, ... 'f' => 15
 *   num        ::= d:digit => d
 *                  n:num '_'? d:digit => n * 10 + d
 *   hexnum     ::= h:hexdigit => h
 *                  n:hexnum '_'? h:hexdigit => n * 16 + h
 * ```
 */
class Num(val sequence: CharSequence, context: ParseContext? = null) {
    var forceHex: Boolean = false

    val foundHexChars: Boolean by lazy { digits.any { it >= 10 } }

    val value: ULong by lazy {
        val multiplier = (if (foundHexChars || forceHex) 16 else 10).toULong()
        var powerVal = 1.toULong()
        var power = 0
        digits.foldRightIndexed(0.toULong()) { _, byteVal, acc ->
            if (byteVal == NumberConstants.UNDERSCORE) return@foldRightIndexed acc
            if (power > 0) {
                powerVal *= multiplier
            }
            power++
            val next = acc + byteVal.toULong() * powerVal
            if (byteVal != 0.toByte() && next < acc) {
                throw ParseException("Invalid constant (constant out of range)", context)
            }
            next
        }
    }

    private val digits: ByteArray by lazy {
        if (sequence.isEmpty()) throw ParseException("Empty number sequence", context)

        val value = ByteArray(sequence.length)
        repeat(sequence.length) { index -> value[index] = sequence.parseDigit(index, context) }
        value
    }

    companion object {
        const val DECIMAL_PATTERN = "([0-9]_?)*[0-9]"
        const val HEX_PATTERN = "([0-9a-fA-F]_?)*[0-9a-fA-F]"
    }
}
