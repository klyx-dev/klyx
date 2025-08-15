package com.klyx.wasm.annotations


@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class HostModule(val name: String)
