@file:OptIn(ExperimentalNativeApi::class)

package com.klyx.editor.compose.text

import org.jetbrains.skia.BreakIterator
import org.jetbrains.skia.icu.CharProperties
import kotlin.experimental.ExperimentalNativeApi
import kotlin.math.absoluteValue
import kotlin.math.sign

internal actual fun String.findPrecedingBreak(index: Int): Int {
    val it = BreakIterator.makeCharacterInstance()
    it.setText(this)
    return it.preceding(index)
}

internal actual fun String.findFollowingBreak(index: Int): Int {
    val it = BreakIterator.makeCharacterInstance()
    it.setText(this)
    return it.following(index)
}

/**
 * Returns the index, in characters, of the code point at distance [offset] from [index].
 *
 * If there aren't enough codepoints in the correct direction, returns 0 (if [offset] is negative)
 * or the length of the char sequence (if [offset] is positive).
 */
internal fun CharSequence.offsetByCodePoints(index: Int, offset: Int): Int {
    val sign = offset.sign
    val distance = offset.absoluteValue

    var currentOffset = index
    for (i in 0 until distance) {
        currentOffset += sign
        if (currentOffset <= 0) return 0
        else if (currentOffset >= length) return length

        val lead = this[currentOffset - 1]
        val trail = this[currentOffset]

        if (isSurrogatePair(lead, trail)) {
            currentOffset += sign
        }
    }

    return currentOffset
}

internal actual fun String.findCodePointOrEmojiStartBefore(index: Int, ifNotFound: Int): Int {
    if (index <= 0) return -1

    // Instead of trying to detect emoji sequences, which is hard, we jump to the preceding break
    // and check whether the codepoint at that index can be the start of an emoji sequence.
    val precedingCharBreakIndex = findPrecedingBreak(index)
    val precedingCodePointIndex = offsetByCodePoints(index, -1)

    // In the very common case of a regular character, avoid the complex computation below
    if (precedingCharBreakIndex == precedingCodePointIndex) return precedingCodePointIndex

    // If the substring between precedingCharBreakIndex and index can be an emoji, then return that
    val substringFromCharBreak = substring(startIndex = precedingCharBreakIndex, endIndex = index)
    return if (canBeEmojiOrPictographic(substringFromCharBreak)) precedingCharBreakIndex
    else precedingCodePointIndex
}

// https://www.unicode.org/reports/tr51/index.html#def_emoji_presentation_selector
// This is needed to detect keycaps. See Emoji_Keycap_Sequence in
// https://unicode.org/Public/emoji/16.0/emoji-sequences.txt
private const val EMOJI_PRESENTATION_SELECTOR = 0xFE0F

private fun canBeEmojiOrPictographic(text: String): Boolean {
    for (codePoint in text.codePoints) {
        with(CharProperties) {
            if (codePointHasBinaryProperty(codePoint, EMOJI_PRESENTATION) ||
                codePointHasBinaryProperty(codePoint, EXTENDED_PICTOGRAPHIC) ||
                codePoint == EMOJI_PRESENTATION_SELECTOR
            ) {
                return true
            }
        }
    }

    return false
}

/**
 * Finds the offset of the next non-whitespace symbols subsequence (word) in the given text
 * starting from the specified caret offset.
 *
 * @param offset The offset where to start looking for the next word.
 * @param currentText The current text in which to search for the next word.
 * @return The offset of the next non-whitespace symbols subsequence (word), or the end of the string
 *         if no such word is found.
 */
internal fun findNextNonWhitespaceSymbolsSubsequenceStartOffset(
    offset: Int,
    currentText: String
): Int {
    /* Assume that next non whitespaces symbols subsequence (word) is when current char is whitespace and next character is not.
     * Emoji (compound incl.) should be treated as a new word.
     */
    val charIterator =
        BreakIterator.makeCharacterInstance() // wordInstance doesn't consider symbols sequence as word
    charIterator.setText(currentText)

    var currentOffset: Int
    var nextOffset = charIterator.next()
    while (nextOffset < offset) {
        nextOffset = charIterator.next()
    }
    currentOffset = nextOffset

    nextOffset = charIterator.next()

    while (nextOffset < currentText.length) { // charIterator.next() works one more time than needed, better use this
        if (currentText.codePointAt(currentOffset).isWhitespace() && !currentText.codePointAt(
                nextOffset
            ).isWhitespace()
        ) {
            return nextOffset
        } else {
            currentOffset = nextOffset
        }

        nextOffset = charIterator.next()
    }
    return currentOffset
}

/**
 * Returns the midpoint position in the string considering Unicode symbols.
 *
 * This function calculates the midpoint position in the given string, taking into account Unicode symbols.
 * It counts the number of symbols in the string to determine the midpoint position.
 *
 * @return The midpoint position in the string.
 */
internal fun String.midpointPositionWithUnicodeSymbols(): Int {
    val symbolsCount = this.codePoints.count()
    val charIterator = BreakIterator.makeCharacterInstance()
    charIterator.setText(this)
    var currentOffset = 0
    for (i in 0..symbolsCount / 2) {
        currentOffset = charIterator.next()
    }
    return currentOffset
}
