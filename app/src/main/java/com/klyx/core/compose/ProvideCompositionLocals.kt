package com.klyx.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.klyx.core.rememberBuildVariant

@Composable
fun ProvideCompositionLocals(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalBuildVariant provides rememberBuildVariant()) {
        content()
    }
}
