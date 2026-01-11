package com.klyx.editor.language.toolchain

import com.klyx.core.app.App
import com.klyx.util.shell.ShellKind
import okio.Path

interface ToolchainLister {
    /**
     * List all available toolchains for a given path.
     */
    suspend fun list(
        worktreeRoot: Path,
        subrootRelativePath: Path,
        projectEnv: Map<String, String>? = null
    ): ToolchainList

    /**
     * Given a user-created toolchain, resolve lister-specific details.
     * Put another way: fill in the details of the toolchain so the user does not have to.
     */
    suspend fun resolve(path: Path, projectEnv: Map<String, String>? = null): Result<Toolchain>

    fun activationScript(toolchain: Toolchain, shell: ShellKind, cx: App): List<String>

    /**
     * Returns various "static" bits of information about this toolchain lister. This function should be pure.
     */
    fun meta(): ToolchainMetadata
}
