package com.klyx.extension.modules

import com.klyx.core.logging.logger
import com.klyx.extension.api.WorktreeRegistry
import com.klyx.extension.modules.impl.worktreeFunctions
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.HostModule
import com.klyx.wasm.HostModuleScope
import com.klyx.wasm.utils.i32

@OptIn(ExperimentalWasmApi::class)
class RootModule : HostModule {
    private val logger = logger()

    override val name = "\$root"

    override fun HostModuleScope.functions() {
        function("[resource-drop]worktree", params = listOf(i32)) { args ->
            runCatching {
                WorktreeRegistry.drop(args.i32.toLong())
            }.onFailure {
                logger.error("Failed to drop worktree: ${it.message}")
            }
        }

        worktreeFunctions()
    }
}
