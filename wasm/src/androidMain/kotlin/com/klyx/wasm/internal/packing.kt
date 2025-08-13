@file:Suppress("NOTHING_TO_INLINE")

package com.klyx.wasm.internal

/** Returns the [Float] value corresponding to a given bit representation. */
fun floatFromBits(bits: Int): Float = Float.fromBits(bits)

/** Returns the [Double] value corresponding to a given bit representation. */
fun doubleFromBits(bits: Long): Double = Double.fromBits(bits)

/** Packs two Float values into one Long value for use in inline classes. */
inline fun packFloats(val1: Float, val2: Float): Long {
    val v1 = val1.toRawBits().toLong()
    val v2 = val2.toRawBits().toLong()
    return (v1 shl 32) or (v2 and 0xFFFFFFFF)
}

/** Unpacks the first Float value in [packFloats] from its returned Long. */
inline fun unpackFloat1(value: Long): Float {
    return floatFromBits((value shr 32).toInt())
}

/** Unpacks the first absolute Float value in [packFloats] from its returned Long. */
inline fun unpackAbsFloat1(value: Long): Float {
    return floatFromBits(((value shr 32) and 0x7FFFFFFF).toInt())
}

/** Unpacks the second Float value in [packFloats] from its returned Long. */
inline fun unpackFloat2(value: Long): Float {
    return floatFromBits((value and 0xFFFFFFFF).toInt())
}

/** Unpacks the second absolute Float value in [packFloats] from its returned Long. */
inline fun unpackAbsFloat2(value: Long): Float {
    return floatFromBits((value and 0x7FFFFFFF).toInt())
}

/** Packs two Int values into one Long value for use in inline classes. */
inline fun packInts(val1: Int, val2: Int): Long {
    return (val1.toLong() shl 32) or (val2.toLong() and 0xFFFFFFFF)
}

/** Unpacks the first Int value in [packInts] from its returned ULong. */
inline fun unpackInt1(value: Long): Int {
    return (value shr 32).toInt()
}

/** Unpacks the second Int value in [packInts] from its returned ULong. */
inline fun unpackInt2(value: Long): Int {
    return (value and 0xFFFFFFFF).toInt()
}
