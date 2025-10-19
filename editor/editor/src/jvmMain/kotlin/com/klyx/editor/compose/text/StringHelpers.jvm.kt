package com.klyx.editor.compose.text

import org.jetbrains.skia.BreakIterator
import org.jetbrains.skia.icu.CharProperties
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
 * Returns true when [high] is a Unicode high-surrogate code unit and [low] is a Unicode
 * low-surrogate code unit.
 */
private fun isSurrogatePair(high: Char, low: Char): Boolean =
    high.isHighSurrogate() && low.isLowSurrogate()

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

// Copied from CharHelpers.skiko.kt
// TODO Remove once it's available in common stdlib https://youtrack.jetbrains.com/issue/KT-23251
internal typealias CodePoint = Int

// Copied from CharHelpers.skiko.kt
/**
 * Converts a surrogate pair to a unicode code point.
 */
internal fun Char.Companion.toCodePoint(high: Char, low: Char): CodePoint =
    (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + MIN_SUPPLEMENTARY_CODE_POINT

// Copy from https://github.com/JetBrains/kotlin/blob/7cd306950aad852e006715067435a4bbd9cd40d2/kotlin-native/runtime/src/main/kotlin/generated/_StringUppercase.kt#L26
internal fun StringBuilder.appendCodePoint(codePoint: Int) {
    if (codePoint < MIN_SUPPLEMENTARY_CODE_POINT) {
        append(codePoint.toChar())
    } else {
        append(Char.MIN_HIGH_SURROGATE + ((codePoint - 0x10000) shr 10))
        append(Char.MIN_LOW_SURROGATE + (codePoint and 0x3ff))
    }
}

// Copied from CharHelpers.skiko.kt
/**
 * The minimum value of a supplementary code point, `\u0x10000`.
 */
private const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000

// Copied from CharHelpers.skiko.kt
internal fun CodePoint.charCount(): Int = if (this >= MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1

// Copied from CharHelpers.skiko.kt
internal val String.codePoints
    get() = codePointsAt(0)

internal fun String.codePointsAt(index: Int) = sequence {
    var current = index
    while (current < length) {
        val codePoint = codePointAt(current)
        yield(codePoint)
        current += codePoint.charCount()
    }
}

// Copied from CharHelpers.skiko.kt
/**
 * Returns the character (Unicode code point) at the specified index.
 */
internal fun CharSequence.codePointAt(index: Int): CodePoint {
    val high = this[index]
    if (high.isHighSurrogate() && index + 1 < this.length) {
        val low = this[index + 1]
        if (low.isLowSurrogate()) {
            return Char.toCodePoint(high, low)
        }
    }
    return high.code
}

// Copied from CharHelpers.skiko.kt
/**
 * Returns the character (Unicode code point) before the specified index.
 */
internal fun CharSequence.codePointBefore(index: Int): CodePoint {
    val low = this[index]
    if (low.isLowSurrogate() && index - 1 >= 0) {
        val high = this[index - 1]
        if (high.isHighSurrogate()) {
            return Char.toCodePoint(high, low)
        }
    }
    return low.code
}

/**
 * Returns the count of Unicode code points.
 */
internal fun CharSequence.codePointCount(): Int {
    var count = length
    var i = 0
    while (i < length - 1) {
        if (this[i].isHighSurrogate() && this[i + 1].isLowSurrogate()) {
            count--
            i += 2
        } else {
            i++
        }
    }
    return count
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
 * Checks if the character at the specified offset in the given string is either a whitespace or punctuation character.
 *
 * @param offset The offset of the character to check.
 * @return `true` if the character is a whitespace or punctuation character, `false` otherwise.
 */
internal fun String.isWhitespaceOrPunctuation(offset: Int): Boolean {
    val codePoint = this.codePointAt(offset)
    return codePoint.isPunctuation() || codePoint.isWhitespace()
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

/**
 * Checks if the given Unicode code point is a whitespace character.
 *
 * @return `true` if the code point is a whitespace character, `false` otherwise.
 */
private fun CodePoint.isWhitespace(): Boolean {
    // TODO: Extend this behavior when (if) Unicode will have compound whitespace characters.
    if (this.charCount() != 1) {
        return false
    }
    return this.toChar().isWhitespace()
}

/**
 * Checks if the given Unicode code point is a punctuation character.
 *
 * @return 'true' if the CodePoint is a punctuation character, 'false' otherwise.
 */
private fun CodePoint.isPunctuation(): Boolean {
    // TODO: Extend this behavior when (if) Unicode will have compound punctuation characters.
    if (this.charCount() != 1) {
        return false
    }
    val punctuationSet = setOf(
        CharCategory.DASH_PUNCTUATION,
        CharCategory.START_PUNCTUATION,
        CharCategory.END_PUNCTUATION,
        CharCategory.CONNECTOR_PUNCTUATION,
        CharCategory.OTHER_PUNCTUATION,
        CharCategory.INITIAL_QUOTE_PUNCTUATION,
        CharCategory.FINAL_QUOTE_PUNCTUATION
    )
    return punctuationSet.any { it.contains(this.toChar()) }
}
