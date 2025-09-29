package com.klyx.editor.compose.text.buffer

import com.klyx.editor.compose.text.buffer.NodeColor.Black
import com.klyx.editor.compose.text.buffer.NodeColor.Red
import kotlinx.serialization.Serializable

@Serializable
internal enum class NodeColor {
    Red, Black
}

@Serializable(with = TreeNodeSerializer::class)
internal data class TreeNode(var piece: Piece, var color: NodeColor) {
    // size of the left subtree (not inorder)
    var sizeLeft = 0

    // line feeds cnt in the left subtree (not in order)
    var lfLeft = 0

    var parent = this
    var left = this
    var right = this

    fun root(): TreeNode {
        var node = this
        while (node.parent !== Sentinel) {
            node = node.parent
        }
        return node
    }

    fun next(): TreeNode {
        if (right !== Sentinel) return right.leftest()

        var node = this
        while (node.parent !== Sentinel) {
            if (node.parent.left === node) {
                break
            }
            node = node.parent
        }

        return if (node.parent === Sentinel) Sentinel else node.parent
    }

    fun prev(): TreeNode {
        if (left !== Sentinel) return left.rightest()

        var node = this
        while (node.parent !== Sentinel) {
            if (node.parent.right === node) {
                break
            }
            node = node.parent
        }

        return if (node.parent === Sentinel) Sentinel else node.parent
    }

    fun detach() {
        this.parent = NullTreeNode
        this.left = NullTreeNode
        this.right = NullTreeNode
    }
}

// null piece
@Serializable
internal val NullPiece = Piece(
    bufferIndex = 0,
    start = BufferCursor(0, 0),
    end = BufferCursor(0, 0),
    lineFeedCnt = 0,
    length = 0
)

// null tree node
@Serializable(with = TreeNodeSerializer::class)
internal val NullTreeNode = TreeNode(NullPiece, color = Red)

// sentinel tree node
@Serializable(with = TreeNodeSerializer::class)
internal val Sentinel: TreeNode = TreeNode(NullPiece, color = Black).apply {
    this.parent = this // parent = Sentinel
    this.left = this // left = Sentinel
    this.right = this // right = Sentinel
    this.color = Black
}

internal fun resetSentinel() {
    Sentinel.parent = Sentinel
}

internal fun TreeNode.leftest(): TreeNode {
    var x = this
    while (x.left !== Sentinel) {
        x = x.left
    }
    return x
}

internal fun TreeNode.rightest(): TreeNode {
    var x = this
    while (x.right !== Sentinel) {
        x = x.right
    }
    return x
}

internal tailrec fun TreeNode.calculateSize(size: Int = 0): Int {
    if (this === Sentinel) return size
    return this.right.calculateSize(size + this.sizeLeft + this.piece.length)
}

internal tailrec fun TreeNode.calculateLF(count: Int = 0): Int {
    if (this === Sentinel) return count
    return this.right.calculateLF(count + this.lfLeft + this.piece.lineFeedCnt)
}

internal fun PieceTreeBase.rotateLeft(x: TreeNode) {
    val y = x.right
    // fix sizeLeft
    y.sizeLeft += x.sizeLeft + x.piece.length
    y.lfLeft += x.lfLeft + x.piece.lineFeedCnt
    x.right = y.left

    if (y.left !== Sentinel) {
        y.left.parent = x
    }
    y.parent = x.parent
    if (x.parent === Sentinel) {
        this.root = y
    } else if (x.parent.left === x) {
        x.parent.left = y
    } else {
        x.parent.right = y
    }
    y.left = x
    x.parent = y
}

internal fun PieceTreeBase.rotateRight(y: TreeNode) {
    val x = y.left
    y.left = x.right
    y.left = x.right
    if (x.right !== Sentinel) {
        x.right.parent = y
    }
    x.parent = y.parent

    // fix sizeLeft
    y.sizeLeft -= x.sizeLeft + x.piece.length
    y.lfLeft -= x.lfLeft + x.piece.lineFeedCnt

    if (y.parent === Sentinel) {
        this.root = x
    } else if (y === y.parent.right) {
        y.parent.right = x
    } else {
        y.parent.left = x
    }

    x.right = y
    y.parent = x
}

internal fun PieceTreeBase.rbDelete(node: TreeNode) {
    var x: TreeNode
    var y: TreeNode
    val z: TreeNode = node

    if (z.left === Sentinel) {
        y = z
        x = y.right
    } else if (z.right === Sentinel) {
        y = z
        x = y.left
    } else {
        y = z.right.leftest()
        x = y.right
    }

    if (y === this.root) {
        this.root = x
        // if x is null, we are removing the only node
        x.color = Black
        z.detach()
        resetSentinel()
        this.root.parent = Sentinel
        return
    }

    val yWasRed = (y.color == Red)

    if (y === y.parent.left) {
        y.parent.left = x
    } else {
        y.parent.right = x
    }

    if (y === z) {
        x.parent = y.parent
        this.recomputeMetadata(x)
    } else {
        if (y.parent === z) {
            x.parent = y
        } else {
            x.parent = y.parent
        }

        // as we make changes to x's hierarchy, update `sizeLeft` of subtree first
        this.recomputeMetadata(x)

        y.left = z.left
        y.right = z.right
        y.parent = z.parent
        y.color = z.color

        if (z === this.root) {
            this.root = y
        } else {
            if (z === z.parent.left) {
                z.parent.left = y
            } else {
                z.parent.right = y
            }
        }

        if (y.left !== Sentinel) {
            y.left.parent = y
        }
        if (y.right !== Sentinel) {
            y.right.parent = y
        }
        // update metadata
        // we replace z with y, so in this sub this, the length change is z.item.length
        y.sizeLeft = z.sizeLeft
        y.lfLeft = z.lfLeft
        this.recomputeMetadata(y)
    }

    z.detach()

    if (x.parent.left === x) {
        val newSizeLeft = x.calculateSize()
        val newLFLeft = x.calculateLF()
        if (newSizeLeft != x.parent.sizeLeft || newLFLeft != x.parent.lfLeft) {
            val delta = newSizeLeft - x.parent.sizeLeft
            val lfDelta = newLFLeft - x.parent.lfLeft
            x.parent.sizeLeft = newSizeLeft
            x.parent.lfLeft = newLFLeft
            this.updateMetadata(x.parent, delta, lfDelta)
        }
    }

    this.recomputeMetadata(x.parent)

    if (yWasRed) {
        resetSentinel()
        return
    }

    // RB-DELETE-FIXUP
    var w: TreeNode
    while (x !== this.root && x.color == Black) {
        if (x === x.parent.left) {
            w = x.parent.right

            if (w.color == Red) {
                w.color = Black
                x.parent.color = Red
                this.rotateLeft(x.parent)
                w = x.parent.right
            }

            if (w.left.color == Black && w.right.color == Black) {
                w.color = Red
                x = x.parent
            } else {
                if (w.right.color == Black) {
                    w.left.color = Black
                    w.color = Red
                    this.rotateRight(w)
                    w = x.parent.right
                }

                w.color = x.parent.color
                x.parent.color = Black
                w.right.color = Black
                this.rotateLeft(x.parent)
                x = this.root
            }
        } else {
            w = x.parent.left

            if (w.color == Red) {
                w.color = Black
                x.parent.color = Red
                this.rotateRight(x.parent)
                w = x.parent.left
            }

            if (w.left.color == Black && w.right.color == Black) {
                w.color = Red
                x = x.parent
            } else {
                if (w.left.color == Black) {
                    w.right.color = Black
                    w.color = Red
                    this.rotateLeft(w)
                    w = x.parent.left
                }

                w.color = x.parent.color
                x.parent.color = Black
                w.left.color = Black
                this.rotateRight(x.parent)
                x = this.root
            }
        }
    }
    x.color = Black
    resetSentinel()
}

internal fun PieceTreeBase.fixInsert(node: TreeNode) {
    var x = node
    this.recomputeMetadata(x)

    while (x !== this.root && x.parent.color == Red) {
        if (x.parent === x.parent.parent.left) {
            val y = x.parent.parent.right

            if (y.color == Red) {
                x.parent.color = Black
                y.color = Black
                x.parent.parent.color = Red
                x = x.parent.parent
            } else {
                if (x === x.parent.right) {
                    x = x.parent
                    this.rotateLeft(x)
                }
                x.parent.color = Black
                x.parent.parent.color = Red
                this.rotateRight(x.parent.parent)
            }
        } else {
            val y = x.parent.parent.left

            if (y.color == Red) {
                x.parent.color = Black
                y.color = Black
                x.parent.parent.color = Red
                x = x.parent.parent
            } else {
                if (x === x.parent.left) {
                    x = x.parent
                    this.rotateRight(x)
                }
                x.parent.color = Black
                x.parent.parent.color = Red
                this.rotateLeft(x.parent.parent)
            }
        }
    }
    this.root.color = Black
}

internal fun PieceTreeBase.updateMetadata(
    node: TreeNode,
    delta: Int,
    lineFeedCntDelta: Int
) {
    var x = node
    // node length change or line feed count change
    while (x !== this.root && x !== Sentinel) {
        if (x.parent.left === x) {
            x.parent.sizeLeft += delta
            x.parent.lfLeft += lineFeedCntDelta
        }
        x = x.parent
    }
}

internal fun PieceTreeBase.recomputeMetadata(node: TreeNode) {
    var delta = 0
    var lfDelta = 0
    var x = node

    if (x === this.root) return

    if (delta == 0) {
        // go upwards till the node whose left subtree is changed.
        while (x !== this.root && x === x.parent.right) {
            x = x.parent
        }

        if (x === this.root) {
            // well, it means we add a node to the end (inorder)
            return
        }

        // x is the node whose right subtree is changed.
        x = x.parent

        delta = x.left.calculateSize() - x.sizeLeft
        lfDelta = x.left.calculateLF() - x.lfLeft
        x.sizeLeft += delta
        x.lfLeft += lfDelta
    }

    // go upwards till root. O(logN)
    while (x !== this.root && (delta != 0 || lfDelta != 0)) {
        if (x.parent.left === x) {
            x.parent.sizeLeft += delta
            x.parent.lfLeft += lfDelta
        }

        x = x.parent
    }
}

internal fun traversalTree(node: TreeNode) {
    if (node !== Sentinel) {
        println(node)
        traversalTree(node.left)
        traversalTree(node.right)
    }
}
