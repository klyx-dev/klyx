package com.klyx.editor.compose.selection.internal

internal actual fun CharSequence.toCharArray(
    destination: CharArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int
) {
    var dstIndex = destinationOffset
    for (srcIndex in startIndex until endIndex) {
        destination[dstIndex++] = this[srcIndex]
    }
}
