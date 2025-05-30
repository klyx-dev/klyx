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

package kwasm.format.text

import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.parseCheckNotNull
import kwasm.format.text.token.Keyword
import kwasm.format.text.token.Paren
import kwasm.format.text.token.Token

/** Determines whether or not the [Token] is a [Keyword] matching the provided [keywordValue]. */
fun Token.isKeyword(keywordValue: String): Boolean =
    this is Keyword && this.value == keywordValue

/**
 * Casts the [Token] into a [Keyword] and returns it if its [Keyword.value] is [value].
 * Returns `null` if either condition is unmet.
 */
fun Token.asKeywordMatching(value: String): Keyword? =
    (this as? Keyword)?.takeIf { it.value == value }

/**
 * Casts the [Token] into a [Keyword] and returns it, along with the match, if its [Keyword.value]
 * matches [regexp].
 *
 * Returns `null` if neither condition is met.
 */
fun Token.asKeywordMatching(regex: Regex): Pair<Keyword, MatchResult>? =
    (this as? Keyword)?.let { regex.matchEntire(it.value) }?.let { this to it }

/** Asserts that the [Token] is a [Keyword] matching the provided [keywordValue]. */
fun Token.assertIsKeyword(keywordValue: String) {
    if (!isKeyword(keywordValue)) throw ParseException("Expected \"$keywordValue\"", context)
}

/**
 * Determines whether the [Token] at the given position is a [Keyword] matching the provided
 * [keyword] value.
 */
fun List<Token>.isKeyword(atIndex: Int, keyword: String): Boolean =
    getOrNull(atIndex)?.isKeyword(keyword) ?: false

/**
 * Helper to check if a [Token] at a given position within the receiving [List] is a [Paren.Open].
 */
fun List<Token>.isOpenParen(atIndex: Int): Boolean = getOrNull(atIndex) is Paren.Open

/**
 * Helper to check if a [Token] at a given position within the receiving [List] is a [Paren.Closed].
 */
fun List<Token>.isClosedParen(atIndex: Int): Boolean = getOrNull(atIndex) is Paren.Closed

/** Helper to get the closest [kwasm.format.ParseContext] from the given position. */
fun List<Token>.contextAt(index: Int): ParseContext? =
    getOrNull(index)?.context ?: index.takeIf { it > 0 }?.let { contextAt(index - 1) }

/**
 * Returns the [Token] as type [T] from the [List] at the given [index], or throws a
 * [ParseException] with the result of the [message] lambda.
 */
inline fun <reified T : Token> List<Token>.getOrThrow(
    index: Int,
    crossinline message: () -> String
): T = parseCheckNotNull(contextAt(index), getOrNull(index) as? T, message)

/**
 * Returns the [Token] as type [T] from the [List] at the given [index], or throws a
 * [ParseException] with the provided [message] (or if null: "Expected [T]".
 */
inline fun <reified T : Token> List<Token>.getOrThrow(
    index: Int,
    message: String? = null
): T = getOrThrow(index) { message ?: "Expected ${T::class.java.simpleName}" }

/**
 * Finds and returns all tokens between the [fromIndex] and when the [expectedClosures]-number of
 * [Paren.Closed] tokens are found (inclusive).
 *
 * For example:
 *
 * ```
 *   (blah (foo (bar) (baz)))
 * ```
 *
 * ```kotlin
 *   myList.tokensUntilParenClosure(1, 1)
 * ```
 *
 * Would return a list of tokens equivalent to `blah (foo (bar) (baz)))`
 */
fun List<Token>.tokensUntilParenClosure(fromIndex: Int, expectedClosures: Int): List<Token> {
    var remainingClosures = expectedClosures
    var currentIndex = fromIndex
    val result = mutableListOf<Token>()

    while (remainingClosures > 0) {
        getOrNull(currentIndex)?.let {
            result.add(it)
            if (it is Paren.Open) {
                remainingClosures++
            } else if (it is Paren.Closed) {
                remainingClosures--
            }
            currentIndex++
        } ?: throw ParseException(
            "Could not find completion of paren closure",
            contextAt(currentIndex)
        )
    }

    return result
}
