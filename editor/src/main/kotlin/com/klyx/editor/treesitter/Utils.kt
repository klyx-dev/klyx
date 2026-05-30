package com.klyx.editor.treesitter

import android.os.Build
import android.util.Log
import io.github.rosemoe.sora.text.CharPosition
import io.github.treesitter.ktreesitter.InputEdit
import io.github.treesitter.ktreesitter.Node
import io.github.treesitter.ktreesitter.Point

/**
 * Convert a [CharPosition] object to a [Point] object
 *
 * Tree-sitter uses UTF-8 byte columns in Point, while Sora editor uses UTF-16 columns.
 * Conversion is needed to ensure Tree-sitter correctly identifies the position in the document.
 */
fun CharPosition.toPoint(mapper: OffsetMapper): Point {
    val lineStartIdx = index - column
    val lineStartByte = mapper.charToByte(lineStartIdx)
    val currentByte = mapper.charToByte(index)
    return Point(line.toUInt(), (currentByte - lineStartByte).toUInt())
}

/**
 * Create a new [InputEdit] object for the given positions
 *
 * Tree-sitter uses UTF-8 byte offsets, while Sora editor uses UTF-16 character indexes.
 * Conversion is required because InputEdit expects byte-based positions for its operation.
 */
fun newInputEdit(
    start: CharPosition,
    oldEnd: CharPosition,
    newEnd: CharPosition,
    mapper: OffsetMapper
) =
    InputEdit(
        mapper.charToByte(start.index).toUInt(),
        mapper.charToByte(oldEnd.index).toUInt(),
        mapper.charToByte(newEnd.index).toUInt(),
        start.toPoint(mapper),
        oldEnd.toPoint(mapper),
        newEnd.toPoint(mapper)
    )

fun Node.printTree(indent: String = "") {
    println("$indent$type [${startPoint.row}, ${startPoint.column}] - [${endPoint.row}, ${endPoint.column}]")

    for (i in 0u until childCount) {
        child(i)?.printTree("$indent  ")
    }
}

inline fun <reified T : AutoCloseable> T.closeSafely() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        runCatching { close() }
            .onFailure {
                Log.w(
                    "TreeSitter",
                    "Failed to close ${T::class.simpleName}",
                    it
                )
            }
    }
}
