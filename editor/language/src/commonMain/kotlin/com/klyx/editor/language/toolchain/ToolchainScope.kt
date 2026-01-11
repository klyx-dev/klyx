package com.klyx.editor.language.toolchain

import okio.Path

/**
 * Declares a scope of a toolchain added by user.
 *
 * When the user adds a toolchain, we give them an option to see that toolchain in:
 * - All of their projects
 * - A project they're currently in.
 * - Only in the subproject they're currently in.
 */
sealed interface ToolchainScope : Comparable<ToolchainScope> {
    data class Subproject(val path: Path, val relativePath: Path) : ToolchainScope {
        init {
            require(relativePath.isRelative) {
                "Relative path must be relative: $relativePath"
            }
        }
    }

    data object Project : ToolchainScope

    /**
     * Available in all projects on this box. It wouldn't make sense to show suggestions across machines.
     */
    data object Global : ToolchainScope

    override fun compareTo(other: ToolchainScope): Int {
        return when (this) {
            is Subproject -> when (other) {
                is Subproject -> compareValuesBy(this, other, { it.path }, { it.relativePath })
                else -> -1 // Subproject comes first
            }

            Project -> when (other) {
                is Subproject -> 1
                Project -> 0
                Global -> -1
            }

            Global -> when (other) {
                Global -> 0
                else -> 1 // Global comes last
            }
        }
    }

    fun label(): String {
        return when (this) {
            is Subproject -> "Subproject"
            Project -> "Project"
            Global -> "Global"
        }
    }

    fun description(): String {
        return when (this) {
            is Subproject -> "Available only in the subproject you're currently in."
            Project -> "Available in all locations in your current project."
            Global -> "Available in all of your projects on this machine."
        }
    }
}
