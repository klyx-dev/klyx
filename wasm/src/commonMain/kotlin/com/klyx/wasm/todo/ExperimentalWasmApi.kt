package com.klyx.wasm.todo

@RequiresOptIn(message = "This API is experimental and may change in the future.")
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class ExperimentalWasmApi
