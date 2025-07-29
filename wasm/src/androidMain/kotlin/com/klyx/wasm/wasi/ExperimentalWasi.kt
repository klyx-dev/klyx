package com.klyx.wasm.wasi

@RequiresOptIn("This API is specific to the WebAssembly System Interface (WASI) and may change in the future.")
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS
)
annotation class ExperimentalWasi()
