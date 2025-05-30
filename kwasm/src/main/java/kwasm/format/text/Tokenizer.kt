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
import kwasm.format.shiftColumnBy
import kwasm.format.text.token.RawToken
import kwasm.format.text.token.Token
import kwasm.format.text.token.findFloatLiteral
import kwasm.format.text.token.findIdentifier
import kwasm.format.text.token.findIntegerLiteral
import kwasm.format.text.token.findKeyword
import kwasm.format.text.token.findParen
import kwasm.format.text.token.findReserved
import kwasm.format.text.token.findStringLiteral
import kwasm.format.text.token.isClosedParen
import kwasm.format.text.token.isFloatLiteral
import kwasm.format.text.token.isIdentifier
import kwasm.format.text.token.isIntegerLiteral
import kwasm.format.text.token.isKeyword
import kwasm.format.text.token.isOpenParen
import kwasm.format.text.token.isReserved
import kwasm.format.text.token.isStringLiteral
import kwasm.format.text.token.toFloatLiteral
import kwasm.format.text.token.toIdentifier
import kwasm.format.text.token.toIntegerLiteral
import kwasm.format.text.token.toKeyword
import kwasm.format.text.token.toParen
import kwasm.format.text.token.toReserved
import kwasm.format.text.token.toStringLiteral
import kwasm.format.text.whitespace.Comment
import kwasm.format.text.whitespace.Format
import java.io.Reader
import java.io.StringReader

/**
 * A tokenizer capable of splitting a raw text-format WASM file into its component tokens.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/lexical.html#tokens):
 *
 * The character stream in the source text is divided, from left to right, into a sequence of
 * tokens, as defined by the following grammar.
 *
 * ```
 *   token      ::= keyword | uN | sN | fN | string | id | ‘(’ | ‘)’ | reserved
 *   keyword    ::= (‘a’ | … |‘z’) idchar* (if occurring as a literal terminal in the grammar)
 *   reserved   ::= idchar+
 * ```
 *
 * Tokens are formed from the input character stream according to the *longest match rule*. That is,
 * the next token always consists of the longest possible sequence of characters that is recognized
 * by the above lexical grammar. Tokens can be separated by white space, but except for strings,
 * they cannot themselves contain whitespace.
 */
class Tokenizer {
    /** Tokenizes source code from the provided [source] [Reader]. */
    fun tokenize(
        source: Reader,
        context: ParseContext? = ParseContext("Unknown File", 1, 1)
    ): List<Token> {
        val rawTokens = source.use { Comment.stripComments(it.readText(), context) }.tokens
        return rawTokens.flatMap(::buildActualTokensFrom)
    }

    /** Tokenizes source code from the provided [source] [String]. */
    fun tokenize(
        source: String,
        context: ParseContext? = ParseContext("Unknown File", 1, 1)
    ): List<Token> = tokenize(StringReader(source), context)

    // TODO: it might be worth considering making a lot of these functions `inline fun`s.
    private fun buildActualTokensFrom(token: RawToken): List<Token> = when {
        token.sequence.isEmpty() -> emptyList()
        // If the entire sequence is whitespace, we can ignore it.
        Format.PATTERN.get().matchEntire(token.sequence) != null -> emptyList()
        token.isIntegerLiteral() -> listOf(token.toIntegerLiteral())
        token.isFloatLiteral() -> listOf(token.toFloatLiteral())
        token.isStringLiteral() -> listOf(token.toStringLiteral())
        token.isIdentifier() -> listOf(token.toIdentifier())
        token.isKeyword() -> listOf(token.toKeyword())
        token.isOpenParen() || token.isClosedParen() -> listOf(token.toParen())
        token.isReserved() -> listOf(token.toReserved())
        else -> {
            val maxLengthTokenFind = listOf(
                token.findStringLiteral(),
                token.findIntegerLiteral(),
                token.findFloatLiteral(),
                token.findKeyword(),
                token.findIdentifier(),
                token.findParen(),
                token.findReserved()
            ).maxByOrNull { it?.sequence?.length ?: -1 }
                ?: throw ParseException(
                    "No valid token found in sequence: \"${token.sequence}\"",
                    token.context
                )

            // Recursively get token list from the contents before the longest found token.
            val prefixTokens = buildActualTokensFrom(
                RawToken(token.sequence.subSequence(0, maxLengthTokenFind.index), token.context)
            )

            // Get the longest found token.
            val longestToken = buildActualTokensFrom(
                RawToken(
                    maxLengthTokenFind.sequence,
                    token.context.shiftColumnBy(maxLengthTokenFind.index)
                )
            )

            // Recursively get token list from the contents after the lontest found token.
            val suffixStart = maxLengthTokenFind.index + maxLengthTokenFind.sequence.length
            val suffixTokens = buildActualTokensFrom(
                RawToken(
                    token.sequence.subSequence(suffixStart, token.sequence.length),
                    token.context.shiftColumnBy(suffixStart)
                )
            )

            prefixTokens + longestToken + suffixTokens
        }
    }
}
