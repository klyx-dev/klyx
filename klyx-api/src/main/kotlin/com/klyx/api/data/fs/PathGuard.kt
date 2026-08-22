package com.klyx.api.data.fs

import java.io.IOException

/**
 * Thrown when a file operation targets a protected path.
 */
class ProtectedPathException(reason: String) : IOException("Protected path: $reason")

class PathGuard private constructor(
    private val exactPaths: Map<String, String>,
    private val prefixPaths: Map<String, String>
) {

    sealed interface Rule {

        /** Blocks exactly [path]; its children remain manageable. */
        data class Exact(val path: String, val reason: String) : Rule

        /** Blocks [path] and everything below it. */
        data class Prefix(val path: String, val reason: String) : Rule
    }

    /**
     * Returns the human-readable reason when [canonicalPath] is protected,
     * or null when deletion/move is allowed.
     */
    fun violation(canonicalPath: String): String? {
        val path = normalize(canonicalPath)

        prefixPaths[path]?.let { return it }
        for ((prefix, reason) in prefixPaths) {
            if (path.startsWith("$prefix/")) return reason
        }
        return exactPaths[path]
    }

    companion object {

        fun of(vararg rules: Rule): PathGuard = PathGuard(
            exactPaths = rules.filterIsInstance<Rule.Exact>()
                .associate { normalize(it.path) to it.reason },
            prefixPaths = rules.filterIsInstance<Rule.Prefix>()
                .associate { normalize(it.path) to it.reason }
        )

        private fun normalize(path: String) =
            path.trimEnd('/').ifEmpty { "/" }
    }
}
