package com.klyx.extension.impl

import com.klyx.extension.ExtensionHostModule
import com.klyx.extension.HostFunctionDefinition
import com.klyx.extension.wasm.toHostFunction
import java.nio.file.Files
import java.nio.file.Paths

class FileSystem : ExtensionHostModule {
    override val namespace get() = "FileSystem"

    override fun getHostFunctions() = listOf(
        HostFunctionDefinition("create_file", ::createFile.toHostFunction())
    )

    private fun createFile(path: String) {
        Files.createFile(Paths.get(path))
    }
}
