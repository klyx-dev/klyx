package com.klyx.core.file

import kotlinx.serialization.Serializable

/**
 * A Klyx project.
 *
 * @property worktrees the worktrees in this project.
 */
@Serializable
data class Project(val worktrees: List<Worktree>) {
    fun isEmpty() = worktrees.isEmpty()
    fun isNotEmpty() = worktrees.isNotEmpty()
}
