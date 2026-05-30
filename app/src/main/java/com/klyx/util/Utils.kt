package com.klyx.util

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import com.klyx.R
import kotlinx.coroutines.CancellationException
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun Throwable.extractMessage() = withApplicationContext {
    if (this is OutOfMemoryError) {
        context.getString(R.string.oom_description)
    } else {
        context.getString(
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
