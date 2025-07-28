package com.klyx.wasm

fun WasmScope.registerHostModule(vararg hostModule: HostModule) {
    for (module in hostModule) {
        hostModule(module.name) {
            with(module) { functions() }
        }
    }
}
