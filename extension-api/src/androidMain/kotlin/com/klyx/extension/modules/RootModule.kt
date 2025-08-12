package com.klyx.extension.modules

import com.klyx.core.borrow.dropPtr
import com.klyx.core.logging.logger
import com.klyx.core.pointer.asPointer
import com.klyx.extension.modules.impl.worktreeFunctions
import com.klyx.wasm.ExperimentalWasmApi
import com.klyx.wasm.HostModule
import com.klyx.wasm.HostModuleScope

@OptIn(ExperimentalWasmApi::class)
class RootModule : HostModule {
    private val logger = logger()

    override val name = "\$root"

    override fun HostModuleScope.functions() {
        function("[resource-drop]worktree", params = listOf(i32)) { args ->
            val ptr = args[0].asPointer()
            if (!dropPtr(ptr)) {
                logger.warn("Failed to drop worktree (ptr: $ptr)")
            }
        }

        worktreeFunctions()
    }
}
