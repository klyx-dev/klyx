package com.klyx.editor.compose.text.buffer

import kotlinx.serialization.Serializable

/**
 * Readonly snapshot for piece tree. In a real multiple thread environment, to make snapshot reading
 * always work correctly, we need to:
 *
 * 1. Make TreeNode.piece immutable, then reading and writing can run in parallel.
 * 2. TreeNode/Buffers normalization should not happen during snapshot reading.
 */
@Serializable
internal class PieceTreeSnapshot(val tree: PieceTreeBase, val BOM: String) {
    private val pieces = mutableListOf<Piece>()
    private var index: Int = 0

    init {
        if (tree.root !== Sentinel) {
            tree.iterateTree { node ->
                if (node !== Sentinel) {
                    pieces.add(node.piece)
                }
                true
            }
        }
    }

    fun read(): String? {
        if (pieces.isEmpty()) {
            return if (index == 0) {
                index++
                BOM
            } else null
        }

        if (index > pieces.lastIndex) {
            return null
        }

        if (index == 0) {
            return BOM + with(tree) { pieces[index++].content() }
        }

        // return the piece content
        return tree.getPieceContent(pieces[index++])
    }
}
