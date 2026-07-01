package com.klyx.api.util

import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.klyx.api.BuildConfig

/**
 * Conditionally applies a [block] of modifiers to the current [Modifier] if [condition] is true.
 */
inline fun Modifier.thenIf(
    condition: Boolean,
    block: Modifier.() -> Modifier
): Modifier {
    return if (condition) block() else this
}

/**
 * Applies either [ifTrue] or [ifFalse] modifiers based on the [condition].
 */
inline fun Modifier.thenIfElse(
    condition: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: Modifier.() -> Modifier
): Modifier {
    return if (condition) ifTrue() else ifFalse()
}

/**
 * Conditionally applies a [block] of modifiers based on the result of the [predicate].
 */
inline fun Modifier.conditional(
    predicate: () -> Boolean,
    block: Modifier.() -> Modifier
): Modifier {
    return if (predicate()) block() else this
}

/**
 * Applies a [block] of modifiers with the non-null [value] if it is not null.
 */
inline fun <T> Modifier.applyIfNotNull(
    value: T?,
    block: Modifier.(T) -> Modifier
): Modifier {
    return if (value != null) {
        block(value)
    } else {
        this
    }
}

/**
 * Applies a [block] of modifiers with the [value] if the [predicate] returns true for that value.
 */
inline fun <T> Modifier.applyIf(
    value: T,
    predicate: (T) -> Boolean,
    block: Modifier.(T) -> Modifier
): Modifier {
    return if (predicate(value)) {
        block(value)
    } else {
        this
    }
}

/**
 * Adds a red border to the [Modifier] for debugging purposes when [enabled] is true.
 * Defaults to true in debug builds.
 */
infix fun Modifier.debugBorder(
    enabled: Boolean = BuildConfig.DEBUG
): Modifier {
    return if (enabled) {
        border(1.dp, Color.Red)
    } else {
        this
    }
}
