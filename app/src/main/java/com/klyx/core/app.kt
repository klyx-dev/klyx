package com.klyx.core

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.klyx.core.compose.ProvideCompositionLocals

fun noLocalProvidedFor(name: String?): Nothing {
    error("CompositionLocal: $name not present")
}

inline fun <reified T : Any> noLocalProvidedFor(): Nothing = noLocalProvidedFor(T::class.simpleName)

fun ComponentActivity.setAppContent(content: @Composable () -> Unit) {
    setContent { ProvideCompositionLocals(content) }
}
