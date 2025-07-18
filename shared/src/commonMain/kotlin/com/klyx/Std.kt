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
