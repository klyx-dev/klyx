package com.klyx.core.compose

import androidx.compose.runtime.compositionLocalOf
import com.klyx.core.BuildVariant
import com.klyx.core.noLocalProvidedFor
import com.klyx.core.settings.AppSettings
import com.klyx.extension.ExtensionFactory

val LocalBuildVariant = compositionLocalOf<BuildVariant> {
    noLocalProvidedFor<BuildVariant>()
}

val LocalAppSettings = compositionLocalOf<AppSettings> {
    noLocalProvidedFor<AppSettings>()
}

val LocalExtensionFactory = compositionLocalOf<ExtensionFactory> {
    noLocalProvidedFor<ExtensionFactory>()
}
