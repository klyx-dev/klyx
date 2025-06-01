package com.klyx.core

fun noLocalProvidedFor(name: String?): Nothing {
    error("CompositionLocal: $name not present")
}

inline fun <reified T : Any> noLocalProvidedFor(): Nothing = noLocalProvidedFor(T::class.simpleName)
