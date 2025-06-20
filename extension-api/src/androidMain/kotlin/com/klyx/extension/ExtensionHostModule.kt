package com.klyx.extension

interface ExtensionHostModule {
    val namespace: String
    fun getHostFunctions(): List<HostFunctionDefinition>
}
