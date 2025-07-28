package com.klyx.wasm

@WasmDsl
fun WasmScope.registerHostModule(vararg hostModule: HostModule) {
    for (module in hostModule) {
        hostModule(module.name) {
            with(module) { functions() }
        }
    }
}
