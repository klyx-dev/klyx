package com.klyx.editor.language.toolchain

import com.klyx.util.partitionPoint

typealias DefaultIndex = UInt

data class ToolchainList(
    val toolchains: List<Toolchain> = emptyList(),
    val default: DefaultIndex? = null,
    val groups: List<Pair<UInt, String>> = emptyList()
) {
    fun defaultToolchain() = default?.toInt()?.let { toolchains.getOrNull(it) }

    fun groupForIndex(index: UInt): Pair<UInt, String>? {
        if (index >= toolchains.size.toUInt()) return null

        val firstEqualOrGreater = groups.partitionPoint { (groupLowerBound, _) -> groupLowerBound <= index }
        return groups.getOrNull(firstEqualOrGreater - 1)
    }
}
