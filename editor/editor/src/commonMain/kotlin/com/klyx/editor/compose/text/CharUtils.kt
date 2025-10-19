@file:JvmName("CharUtilsKt")

package com.klyx.editor.compose.text

import kotlin.jvm.JvmName

/**
 * The minimum value of a
 * [Unicode supplementary code point](http://www.unicode.org/glossary/#supplementary_code_point), constant `U+10000`.
 */
private const val MIN_SUPPLEMENTARY_CODE_POINT = 0x010000

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

private fun toCodePoint(high: Char, low: Char): Int {
    return ((high.code shl 10) + low.code) + ((MIN_SUPPLEMENTARY_CODE_POINT
            - (Char.MIN_HIGH_SURROGATE.code shl 10)
            - Char.MIN_LOW_SURROGATE.code))
}

/**
 * Determines whether the specified character (Unicode code point)
 * is in the [Basic Multilingual Plane (BMP)](#BMP).
 * Such code points can be represented using a single `char`.
 *
 * @param  codePoint the character (Unicode code point) to be to
 * @return `true` if the specified code point is between
 * [Char.MIN_VALUE] and [Char.MAX_VALUE] inclusive;
 * `false` otherwise.
 */
fun Char.Companion.isBmpCodePoint(codePoint: Int): Boolean {
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
fun Char.Companion.isValidCodePoint(codePoint: Int): Boolean {
    val plane = codePoint ushr 16
    return plane < ((MAX_CODE_POINT + 1) ushr 16)
}

fun Char.Companion.highSurrogate(codePoint: Int): Char {
    return ((codePoint ushr 10)
            + (Char.MIN_HIGH_SURROGATE.code - (MIN_SUPPLEMENTARY_CODE_POINT ushr 10))).toChar()
}

fun Char.Companion.lowSurrogate(codePoint: Int): Char {
    return ((codePoint and 0x3ff) + Char.MIN_LOW_SURROGATE.code).toChar()
}

private fun Int.toSurrogates(dst: CharArray, index: Int) {
    // We write elements "backwards" to guarantee all-or-nothing
    dst[index + 1] = Char.lowSurrogate(this)
    dst[index] = Char.highSurrogate(this)
}

private fun codePointAt1(seq: CharSequence, index: Int): Int {
    var index = index
    val c1 = seq[index]
    if (c1.isHighSurrogate() && ++index < seq.length) {
        val c2 = seq[index]
        if (c2.isLowSurrogate()) {
            return toCodePoint(c1, c2)
        }
    }
    return c1.code
}

//fun CharSequence.codePointAt(index: Int) = codePointAt1(this, index)
fun Char.Companion.codePointAt(seq: CharSequence, index: Int) = codePointAt1(seq, index)

private fun codePointToChars(codePoint: Int): CharArray {
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

fun Int.toChars() = codePointToChars(this)

