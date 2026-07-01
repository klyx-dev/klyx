package com.klyx.api.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.klyx.api.R
import kotlinx.coroutines.CancellationException
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

/**
 * Extracts a user-friendly error message from a [Throwable].
 *
 * If the error is an [OutOfMemoryError], it returns a specific OOM description.
 * Otherwise, it attempts to return the localized message or a generic error string.
 */
fun Throwable.extractMessage() = withApplicationContext {
    if (this@extractMessage is OutOfMemoryError) {
        getString(R.string.oom_description)
    } else {
        getString(
            R.string.smth_went_wrong,
            (localizedMessage?.takeIf { it.isNotBlank() } ?: message)?.decodeEscaped().orEmpty()
                .ifEmpty {
                    this::class.java.simpleName
                }
        )
    }
}

/**
 * Recursively searches for an [Activity] context starting from the current [Context].
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Executes the given [block] and returns its result, or null if an exception (other than [CancellationException]) occurs.
 */
inline fun <T> tryOrNull(block: () -> T): T? {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }

    return try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Throwable) {
        null
    }
}

/**
 * Calculates the number of steps required for a slider given a specific [increment].
 */
fun ClosedFloatingPointRange<Float>.sliderSteps(
    increment: Float
): Int {
    require(increment > 0f)
    return ((endInclusive - start) / increment).roundToInt() - 1
}
