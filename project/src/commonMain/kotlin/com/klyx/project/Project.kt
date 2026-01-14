package com.klyx.project

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * A Klyx project.
 *
 * @property worktrees the worktrees in this project.
 */
@Immutable
@Serializable
data class Project(val worktrees: List<Worktree>) {
    fun isEmpty() = worktrees.isEmpty()
    fun isNotEmpty() = worktrees.isNotEmpty()
}
