package com.klyx.wasm.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class HostFunction(val name: String = "")
