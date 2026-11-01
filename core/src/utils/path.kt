package com.klyx.core.utils

import kotlinx.io.files.Path

@Suppress("NOTHING_TO_INLINE")
inline fun Path.join(vararg parts: String): Path = Path(this, parts = parts)

fun Path.join(vararg parts: Path): Path = Path(this, parts = parts.map { it.toString() }.toTypedArray())

/**
 * Calculates the relative path from [base] to this path.
 *
 * The base path is treated as a directory.
 *
 * @throws IllegalArgumentException if paths have different roots.
 */
fun Path.relativeTo(base: Path): Path {
    val targetParts = normalizedSegments()
    val baseParts = base.normalizedSegments()

    require(isAbsolute == base.isAbsolute) {
        "Paths must have the same root type."
    }

    var common = 0
    val maxCommon = minOf(targetParts.size, baseParts.size)

    while (common < maxCommon && targetParts[common] == baseParts[common]) {
        common++
    }

    val result = buildList {
        repeat(baseParts.size - common) {
            add("..")
        }

        addAll(targetParts.drop(common))
    }

    return Path(result.joinToString(separator = "/"))
}

private fun Path.normalizedSegments(): List<String> {
    val result = mutableListOf<String>()
    toString()
        .replace('\\', '/')
        .split('/')
        .filter(String::isNotEmpty)
        .forEach { segment ->
            when (segment) {
                "." -> Unit
                ".." -> {
                    if (result.isNotEmpty() && result.last() != "..") {
                        result.removeLast()
                    } else {
                        result.add("..")
                    }
                }

                else -> result.add(segment)
            }
        }
    return result
}
