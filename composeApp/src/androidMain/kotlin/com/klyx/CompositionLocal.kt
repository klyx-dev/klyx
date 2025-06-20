package com.klyx

import androidx.compose.runtime.compositionLocalOf
import com.klyx.core.noLocalProvidedFor
import com.klyx.extension.ExtensionFactory

val LocalExtensionFactory = compositionLocalOf<ExtensionFactory> {
    noLocalProvidedFor<ExtensionFactory>()
}
