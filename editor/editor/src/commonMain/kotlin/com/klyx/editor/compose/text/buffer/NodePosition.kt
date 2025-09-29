package com.klyx.editor.compose.text.buffer

import kotlinx.serialization.Serializable

@Serializable
internal data class NodePosition(
    @Serializable(with = TreeNodeSerializer::class)
    var node: TreeNode, // Piece Index
    var remainder: Int, // remainder in current piece
    var nodeStartOffset: Int // node start offset in document
)

@Serializable
internal val NullNodePosition = NodePosition(NullTreeNode, 0, 0)
