package com.klyx.editor.compose.selection.internal

internal actual fun CharSequence.toCharArray(
    destination: CharArray,
    destinationOffset: Int,
    startIndex: Int,
    endIndex: Int
) {
    rangeCheck(start = startIndex, end = endIndex, length = length) {
        "Expected source [$startIndex, $endIndex) to be in [0, $length)"
    }
    val copyLength = endIndex - startIndex
    rangeCheck(
        start = destinationOffset,
        end = destinationOffset + copyLength,
        length = destination.size
    ) {
        "Expected destination [$destinationOffset, ${destinationOffset + copyLength}) " +
                "to be in [0, ${destination.size})"
    }

    for (i in 0 until copyLength) {
        destination[destinationOffset + i] = get(startIndex + i)
    }
}

private inline fun rangeCheck(start: Int, end: Int, length: Int, lazyMessage: () -> String) {
    require((start >= 0) && (start <= end) && (end <= length), lazyMessage)
}
