package com.klyx.api.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import com.klyx.api.R
import kotlinx.coroutines.CancellationException
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.math.roundToInt

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

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

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

fun ClosedFloatingPointRange<Float>.sliderSteps(
    increment: Float
): Int {
    require(increment > 0f)
    return ((endInclusive - start) / increment).roundToInt() - 1
}
