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

package kwasm.format.text.whitespace

import kwasm.format.ParseContext
import kwasm.format.ParseException
import kwasm.format.shiftColumnBy
import kwasm.format.shiftLineNumberBy
import kwasm.format.text.token.RawToken

/**
 * Comments are effectively whitespace when it comes time to interpret the code.
 *
 * From [the docs](https://webassembly.github.io/spec/core/text/lexical.html#comments):
 *
 * ```
 *   comment        ::= linecomment | bockcomment
 *   linecomment    ::= ‘;;’ linechar∗ (U+0A | eof)
 *   linechar       ::= c:char                      (if c ≠ U+0A)
 *   blockcomment   ::= ‘(;’ blockchar∗ ‘;)’
 *   blockchar      ::= c:char (if c ≠ ‘;’ ∧ c ≠ ‘(’)
 *                      ‘;’ (if the next character is not ‘)’)
 *                      ‘(’ (if the next character is not ‘;’)
 *                      blockcomment
 * ```
 */
object Comment {
    /** Strips all comments from the [source] code. */
    // TODO: may need to use codepoints, rather than chars?
    fun stripComments(source: CharSequence, context: ParseContext? = null): StripResult {
        // Place to store the actual source-lines.
        val lines = mutableListOf<RawToken>()
        // Accumulator for the current source-line's contents.
        val lineBuilder = StringBuilder()

        var currentContext = context
        var currentLineStartColumn = 1
        var currentIndex = 0
        while (currentIndex < source.length) {
            val indexAfterComments = indexAfterComments(source, currentIndex, currentContext)

            if (indexAfterComments == currentIndex) {
                // No comments found at the current index. We need to inch-along.
                if (source[currentIndex] == '\n') {
                    // It's a line break. Let's stash the current line's tokens, clear out the
                    // builder, increment our currentContext's lineNumber, and reset the column
                    // counters.

                    val line = lineBuilder.toString()
                    lines.add(RawToken(line, currentContext?.copy(column = currentLineStartColumn)))
                    lineBuilder.clear()

                    currentContext = currentContext.shiftLineNumberBy(1)
                    currentLineStartColumn = 1
                } else {
                    // Not a comment, and not a line break, so we need to append to our lineBuilder.
                    lineBuilder.append(source[currentIndex])
                }

                // Do the inching-along part.
                currentIndex++
            } else {
                // There was a comment at the currentIndex. Let's update the current context by
                // shifting for the nubmer of newlines between the beginning and end of the comment
                // as well as the column index of the end of the comment.

                if (lineBuilder.isNotEmpty()) {
                    val line = lineBuilder.toString()
                    lines.add(RawToken(line, currentContext?.copy(column = currentLineStartColumn)))
                    lineBuilder.clear()
                }
                currentLineStartColumn = source.columnNumberFor(indexAfterComments)

                currentContext = currentContext
                    .shiftLineNumberBy(source.newlinesBetween(currentIndex, indexAfterComments))
                    .shiftColumnBy(currentLineStartColumn)

                currentIndex = indexAfterComments
            }
        }

        if (lineBuilder.isNotEmpty()) {
            val line = lineBuilder.toString()
            lines.add(RawToken(line, currentContext?.copy(column = currentLineStartColumn)))
        }

        return StripResult(lines)
    }

    private fun CharSequence.newlinesBetween(start: Int, end: Int): Int {
        var count = 0
        (start until end).forEach {
            if (this[it] == '\n') count++
        }
        return count
    }

    private fun CharSequence.columnNumberFor(index: Int): Int {
        var columnCount = 1
        var currentIndex = index
        while (currentIndex > 0 && this[currentIndex - 1] != '\n') {
            currentIndex--
            columnCount++
        }
        return columnCount
    }

    /** Finds the index of the first character after any comments at the start of the sequence. */
    private fun indexAfterComments(
        sequence: CharSequence,
        startIndex: Int,
        context: ParseContext? = null
    ): Int {
        val lineCommentFind = LINE_COMMENT_PATTERN.get().find(sequence, startIndex)
        if (lineCommentFind != null && lineCommentFind.range.first == startIndex) {
            // Look for any comments immediately after the line comment.
            return indexAfterComments(
                sequence,
                lineCommentFind.range.last + 1,
                context.shiftColumnBy(lineCommentFind.value.length)
            )
        }

        val indexAfterBlockComment = indexAfterBlockComment(sequence, startIndex, context)
        // Didnt find any block comments starting at startIndex, so return.
        if (indexAfterBlockComment == startIndex) return startIndex

        // Make sure this comment isn't immediately followed by another.
        return indexAfterComments(
            sequence,
            indexAfterBlockComment,
            context.shiftColumnBy(indexAfterBlockComment)
        )
    }

    private val LINE_COMMENT_PATTERN = object : ThreadLocal<Regex>() {
        override fun initialValue(): Regex = ";;[^\n]*".toRegex(RegexOption.MULTILINE)
    }

    private fun indexAfterBlockComment(
        sequence: CharSequence,
        startIndex: Int,
        context: ParseContext? = null
    ): Int {
        // Not enough space for a block comment.
        if (sequence.length - startIndex < 4) return startIndex
        // The first two chars aren't opening a block comment.
        if (sequence[startIndex] != '(' || sequence[startIndex + 1] != ';') return startIndex

        val blockCommentOpeners = arrayListOf(Unit)
        var currentIndex = startIndex + 2
        while (currentIndex < sequence.length && blockCommentOpeners.isNotEmpty()) {
            val c = sequence[currentIndex]
            currentIndex += when (c) {
                '(' -> {
                    if (sequence.hasOneMore(currentIndex) && sequence[currentIndex + 1] == ';') {
                        blockCommentOpeners += Unit
                        2
                    } else 1
                }
                ';' -> {
                    if (sequence.hasOneMore(currentIndex) && sequence[currentIndex + 1] == ')') {
                        blockCommentOpeners.removeAt(blockCommentOpeners.size - 1)
                        2
                    } else 1
                }
                else -> 1
            }
        }

        return currentIndex.takeIf { blockCommentOpeners.isEmpty() }
            ?: throw ParseException("Expected end of block comment", context)
    }

    private fun CharSequence.hasOneMore(afterIndex: Int): Boolean = length - afterIndex > 1

    /** Represents all of the raw tokens found after stripping all comments from the source. */
    data class StripResult(val tokens: List<RawToken>)
}
