package com.klyx.editor.compose.text.buffer

import kotlinx.serialization.Serializable

@Serializable
internal data class CacheEntry(
    @Serializable(with = TreeNodeSerializer::class)
    var node: TreeNode,
    var nodeStartOffset: Int,
    var nodeStartLineNumber: Int
)
