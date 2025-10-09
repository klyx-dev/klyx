package com.klyx.editor.compose

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

// This function exists so we do *not* inline the throw. It keeps
// the call site much smaller and since it's the slow path anyway,
// we don't mind the extra function call
internal fun throwIllegalStateException(message: String) {
    throw IllegalStateException(message)
}

internal fun throwIllegalStateExceptionForNullCheck(message: String): Nothing {
    throw IllegalStateException(message)
}

internal fun throwIllegalArgumentException(message: String) {
    throw IllegalArgumentException(message)
}

internal fun throwIllegalArgumentExceptionForNullCheck(message: String): Nothing {
    throw IllegalArgumentException(message)
}

internal fun throwIndexOutOfBoundsException(message: String) {
    throw IndexOutOfBoundsException(message)
}

// Like Kotlin's check() but without the .toString() call and
// a non-inline throw
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun checkPrecondition(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalStateException(lazyMessage())
    }
}

@Suppress("NOTHING_TO_INLINE", "BanInlineOptIn", "KotlinRedundantDiagnosticSuppress")
@OptIn(ExperimentalContracts::class)
internal inline fun checkPrecondition(value: Boolean) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalStateException("Check failed.")
    }
}

// Like Kotlin's checkNotNull() but without the .toString() call and
// a non-inline throw
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> checkPreconditionNotNull(value: T?, lazyMessage: () -> String): T {
    contract { returns() implies (value != null) }

    if (value == null) {
        throwIllegalStateExceptionForNullCheck(lazyMessage())
    }

    return value
}

// Like Kotlin's checkNotNull() but with a non-inline throw
@Suppress("NOTHING_TO_INLINE", "BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> checkPreconditionNotNull(value: T?): T {
    contract { returns() implies (value != null) }

    if (value == null) {
        throwIllegalStateExceptionForNullCheck("Required value was null.")
    }

    return value
}

// Like Kotlin's require() but without the .toString() call
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class) // same opt-in as using Kotlin's require()
internal inline fun requirePrecondition(value: Boolean, lazyMessage: () -> String) {
    contract { returns() implies value }
    if (!value) {
        throwIllegalArgumentException(lazyMessage())
    }
}

// Like Kotlin's checkNotNull() but without the .toString() call and
// a non-inline throw
@Suppress("BanInlineOptIn")
@OptIn(ExperimentalContracts::class)
internal inline fun <T : Any> requirePreconditionNotNull(value: T?, lazyMessage: () -> String): T {
    contract { returns() implies (value != null) }

    if (value == null) {
        throwIllegalArgumentExceptionForNullCheck(lazyMessage())
    }

    return value
}
