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
import kwasm.format.text.token.util.TokenMatchResult

/** Representations of Open/Close Parentheses in source WebAssembly. */
sealed class Paren : Token {
    /** Representation of `(`, and its location within the source. */
    @Suppress("EqualsOrHashCode") // This is intentional.
    class Open(override val context: ParseContext? = null) : Paren() {
        override fun equals(other: Any?): Boolean = other is Open
    }

    /** Representation of `)`, and its location within the source. */
    @Suppress("EqualsOrHashCode") // This is intentional.
    class Closed(override val context: ParseContext? = null) : Paren() {
        override fun equals(other: Any?): Boolean = other is Closed
    }
}

fun RawToken.findParen(): TokenMatchResult? {
    sequence.indexOf('(').takeIf { it >= 0 }
        ?.let { return TokenMatchResult(it, "(") }
    sequence.indexOf(')').takeIf { it >= 0 }
        ?.let { return TokenMatchResult(it, ")") }
    return null
}

fun RawToken.isOpenParen(): Boolean = sequence == "("

fun RawToken.isClosedParen(): Boolean = sequence == ")"

fun RawToken.toParen() = if (isOpenParen()) Paren.Open(context) else Paren.Closed(context)
