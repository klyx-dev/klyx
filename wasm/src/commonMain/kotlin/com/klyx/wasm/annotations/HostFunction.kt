package com.klyx.wasm.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class HostFunction(
    val name: String = "",
    val case: HostFunctionCase = HostFunctionCase.KebabCase
)

// https://en.wikipedia.org/wiki/Naming_convention_(programming)
enum class HostFunctionCase {
    KebabCase, CamelCase, PascalCase, SnakeCase, ScreamingSnakeCase,
}
