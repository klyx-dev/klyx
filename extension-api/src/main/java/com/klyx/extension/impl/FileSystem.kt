package com.klyx.extension.impl

import com.klyx.extension.ExtensionHostModule
import com.klyx.extension.HostFunctionDefinition

class FileSystem : ExtensionHostModule {
    override val namespace get() = "FileSystem"

    override fun getHostFunctions(): List<HostFunctionDefinition> {
        return listOf()
    }

    private fun createFile(path: String) {

    }
}
