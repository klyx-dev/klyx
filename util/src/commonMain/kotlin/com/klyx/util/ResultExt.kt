@file:Suppress("FunctionName")

package com.klyx.util

inline fun <reified T> Ok(value: T): Result<T> = Result.success(value)

@Suppress("NOTHING_TO_INLINE")
inline fun Err(error: Throwable): Result<Nothing> = Result.failure(error)

@Suppress("NOTHING_TO_INLINE")
inline fun Err(message: String, cause: Throwable? = null): Result<Nothing> =
    Result.failure(RuntimeException(message, cause))

inline fun <T> Result<T>.getOrThrow(
    transform: (Throwable) -> Throwable
): T = fold({ it }, { throw transform(it) })
