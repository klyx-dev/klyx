package com.klyx

/**
 * Unwraps a result, yielding the content of an [Result.success].
 *
 * @param message the message to use if the value is an [Result.failure]
 * @return the value of the [Result.success]
 * @throws IllegalStateException if the value is an [Result.failure]
 */
fun <T> Result<T>.expect(message: String): T {
    return getOrElse { throw IllegalStateException(message, it) }
}

fun <T> Array<T>.takePair(offset: Int = 0): Pair<T, T> {
    return get(offset) to get(offset + 1)
}

fun <T> Array<T>.takeTriple(offset: Int = 0): Triple<T, T, T> {
    return tripleOf(get(offset), get(offset + 1), get(offset + 2))
}

fun <A, B, C> tripleOf(a: A, b: B, c: C) = Triple(a, b, c)

fun <A, B, C> Pair<A, B>.toTriple(c: C) = Triple(first, second, c)

fun <T> T?.asResult(): Result<T> = run {
    if (this != null) Result.success(this) else Result.failure(NullPointerException("Value is null"))
}

fun Pair<Long, Long>.asIntPair() = first.toInt() to second.toInt()
fun Triple<Long, Long, Long>.asIntTriple() = first.toInt() to second.toInt() to third.toInt()
