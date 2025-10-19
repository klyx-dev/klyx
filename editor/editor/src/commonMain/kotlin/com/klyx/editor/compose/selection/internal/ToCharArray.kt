package com.klyx.editor.compose.selection.internal

/**
 * Copies characters from this [CharSequence] into [destination].
 *
 * Platform-specific implementations should use native functions for performing this operation if
 * they exist, since they will likely be more efficient than copying each character individually.
 *
 * @param destination The [CharArray] to copy into.
 * @param destinationOffset The index in [destination] to start copying to.
 * @param startIndex The index in `this` of the first character to copy from (inclusive).
 * @param endIndex The index in `this` of the last character to copy from (exclusive).
 */
internal expect fun CharSequence.toCharArray(
    destination: CharArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int,
)
