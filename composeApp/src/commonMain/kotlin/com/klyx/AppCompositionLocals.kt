package com.klyx

import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.window.core.layout.WindowSizeClass
import com.klyx.core.SharedLocalProvider
import com.klyx.core.noLocalProvidedFor
import com.klyx.di.ProvideViewModels

val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> { noLocalProvidedFor<WindowSizeClass>() }

@Composable
fun ProvideCompositionLocals(content: @Composable (() -> Unit)) {
    SharedLocalProvider {
        ProvideViewModels {
            val windowSizeClass = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true).windowSizeClass

            CompositionLocalProvider(
                LocalWindowSizeClass provides windowSizeClass,
                content = content
            )
        }
    }
}
