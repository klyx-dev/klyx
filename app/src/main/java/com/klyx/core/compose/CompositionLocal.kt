package com.klyx.core.compose

import androidx.compose.runtime.compositionLocalOf
import com.klyx.core.BuildVariant
import com.klyx.core.noLocalProvidedFor

val LocalBuildVariant = compositionLocalOf<BuildVariant> {
    noLocalProvidedFor<BuildVariant>()
}
