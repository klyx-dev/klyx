package com.klyx

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import com.klyx.core.SharedLocalProvider
import com.klyx.core.noLocalProvidedFor
import com.klyx.di.ProvideViewModels

val LocalWindowAdaptiveInfo = compositionLocalOf<WindowAdaptiveInfo> {
    noLocalProvidedFor("LocalWindowAdaptiveInfo")
}

val LocalWindowSizeClass = compositionLocalWithComputedDefaultOf {
    LocalWindowAdaptiveInfo.currentValue.windowSizeClass
}

@Composable
fun ProvideCompositionLocals(content: @Composable (() -> Unit)) {
    SharedLocalProvider {
        ProvideViewModels {
            val windowAdaptiveInfo = currentWindowAdaptiveInfo(supportLargeAndXLargeWidth = true)

            CompositionLocalProvider(
                LocalWindowAdaptiveInfo provides windowAdaptiveInfo,
                content = content
            )
        }
    }
}
