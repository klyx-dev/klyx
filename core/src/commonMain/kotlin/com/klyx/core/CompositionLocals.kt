package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.settings.AppSettings

fun noLocalProvidedFor(name: String?): Nothing {
    error("CompositionLocal: $name not present")
}

inline fun <reified T : Any> noLocalProvidedFor(): Nothing = noLocalProvidedFor(T::class.simpleName)

@Composable
expect fun ProvideBaseCompositionLocals(content: @Composable () -> Unit)

val LocalAppSettings = compositionLocalOf<AppSettings> {
    noLocalProvidedFor<AppSettings>()
}

val LocalBuildVariant = staticCompositionLocalOf<BuildVariant> {
    noLocalProvidedFor<BuildVariant>()
}
