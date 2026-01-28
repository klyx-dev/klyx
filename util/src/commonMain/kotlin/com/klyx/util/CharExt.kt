package com.klyx.util


// Copied from CharHelpers.skiko.kt
// TODO Remove once it's available in common stdlib https://youtrack.jetbrains.com/issue/KT-23251
typealias CodePoint = Int

// Copied from CharHelpers.skiko.kt
/**
 * Converts a surrogate pair to a unicode code point.
 */
fun Char.Companion.toCodePoint(high: Char, low: Char): CodePoint =
    (((high - MIN_HIGH_SURROGATE) shl 10) or (low - MIN_LOW_SURROGATE)) + MIN_SUPPLEMENTARY_CODE_POINT

// Copy from https://github.com/JetBrains/kotlin/blob/7cd306950aad852e006715067435a4bbd9cd40d2/kotlin-native/runtime/src/main/kotlin/generated/_StringUppercase.kt#L26
fun StringBuilder.appendCodePoint(codePoint: Int) {
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
const val MIN_SUPPLEMENTARY_CODE_POINT: Int = 0x10000

/**
 * The minimum value of a
 * [Unicode code point](http://www.unicode.org/glossary/#code_point), constant `U+0000`.
 */
private const val MIN_CODE_POINT = 0x000000

/**
 * The maximum value of a
 * [Unicode code point](http://www.unicode.org/glossary/#code_point), constant `U+10FFFF`.
 */
private const val MAX_CODE_POINT = 0X10FFFF

// Copied from CharHelpers.skiko.kt
fun CodePoint.charCount(): Int = if (this >= MIN_SUPPLEMENTARY_CODE_POINT) 2 else 1

// Copied from CharHelpers.skiko.kt
val String.codePoints
    get() = codePointsAt(0)

fun String.codePointsAt(index: Int) = sequence {
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
fun CharSequence.codePointAt(index: Int): CodePoint {
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
fun CharSequence.codePointBefore(index: Int): CodePoint {
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
fun CharSequence.codePointCount(): Int {
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
 * Checks if the character at the specified offset in the given string is either a whitespace or punctuation character.
 *
 * @param offset The offset of the character to check.
 * @return `true` if the character is a whitespace or punctuation character, `false` otherwise.
 */
fun String.isWhitespaceOrPunctuation(offset: Int): Boolean {
    val codePoint = this.codePointAt(offset)
    return codePoint.isPunctuation() || codePoint.isWhitespace()
}

/**
 * Checks if the given Unicode code point is a whitespace character.
 *
 * @return `true` if the code point is a whitespace character, `false` otherwise.
 */
fun CodePoint.isWhitespace(): Boolean {
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
fun CodePoint.isPunctuation(): Boolean {
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

/**
 * Returns true when [high] is a Unicode high-surrogate code unit and [low] is a Unicode
 * low-surrogate code unit.
 */
fun isSurrogatePair(high: Char, low: Char): Boolean = high.isHighSurrogate() && low.isLowSurrogate()

/**
 * Determines whether the specified character (Unicode code point)
 * is on the [Basic Multilingual Plane (BMP)](#BMP).
 * Such code points can be represented using a single `char`.
 *
 * @param  codePoint the character (Unicode code point) to be to
 * @return `true` if the specified code point is between
 * [Char.MIN_VALUE] and [Char.MAX_VALUE] inclusive;
 * `false` otherwise.
 */
fun Char.Companion.isBmpCodePoint(codePoint: CodePoint): Boolean {
    return codePoint ushr 16 == 0
}

/**
 * Determines whether the specified code point is a valid
 * [Unicode code point value](http://www.unicode.org/glossary/#code_point).
 *
 * @param  codePoint the Unicode code point to be tested
 * @return `true` if the specified code point value is between
 * [MIN_CODE_POINT] and [MAX_CODE_POINT] inclusive;
 * `false` otherwise.
 */
fun Char.Companion.isValidCodePoint(codePoint: CodePoint): Boolean {
    val plane = codePoint ushr 16
    return plane < ((MAX_CODE_POINT + 1) ushr 16)
}

fun CodePoint.lowSurrogate(): Char = ((this and 0x3ff) + Char.MIN_LOW_SURROGATE.code).toChar()

fun CodePoint.highSurrogate(): Char = ((this ushr 10)
        + (Char.MIN_HIGH_SURROGATE.code - (MIN_SUPPLEMENTARY_CODE_POINT ushr 10))).toChar()

private fun CodePoint.toSurrogates(dst: CharArray, index: Int) {
    // We write elements "backwards" to guarantee all-or-nothing
    dst[index + 1] = this.lowSurrogate()
    dst[index] = this.highSurrogate()
}

private fun codePointToChars(codePoint: CodePoint): CharArray {
    return when {
        Char.isBmpCodePoint(codePoint) -> charArrayOf(codePoint.toChar())
        Char.isValidCodePoint(codePoint) -> {
            val result = CharArray(2)
            codePoint.toSurrogates(result, 0)
            result
        }

        else -> error("Not a valid Unicode code point: $codePoint")
    }
}

fun CodePoint.toChars(): CharArray {
    val result = CharArray(2)
    this.toSurrogates(result, 0)
    return result
}

/**
 * Converts the specified character (Unicode code point) to its
 * UTF-16 representation. If the specified code point is a BMP
 * (Basic Multilingual Plane or Plane 0) value, the same value is
 * stored in `dst[dstIndex]`, and 1 is returned. If the
 * specified code point is a supplementary character, its
 * surrogate values are stored in `dst[dstIndex]`
 * (high-surrogate) and `dst[dstIndex+1]`
 * (low-surrogate), and 2 is returned.
 *
 * @receiver the character (Unicode code point) to be converted.
 * @param  dst a [CharArray] in which the `codePoint`'s UTF-16 value is stored.
 * @param dstIndex the start index into the `dst` array where the converted value is stored.
 * @return 1 if the code point is a BMP code point, 2 if the
 *          code point is a supplementary code point.
 *
 * @throws IllegalArgumentException if this codepoint is not a valid Unicode code point.
 * @throws IndexOutOfBoundsException if `dstIndex` is negative or not less than `dst.size`, or if
 * `dst` at `dstIndex` doesn't have enough array element(s) to store the resulting [Char]
 * value(s). (If `dstIndex` is equal to `dst.length-1` and the [codePoint][this] is a supplementary character, the
 * high-surrogate value is not stored in `dst[dstIndex]`.)
 */
@IgnorableReturnValue
fun CodePoint.toChars(dst: CharArray, dstIndex: Int): Int {
    if (Char.isBmpCodePoint(this)) {
        dst[dstIndex] = this.toChar()
        return 1
    } else if (Char.isValidCodePoint(this)) {
        this.toSurrogates(dst, dstIndex)
        return 2
    } else {
        throw IllegalArgumentException("Not a valid Unicode code point: $this")
    }
}
