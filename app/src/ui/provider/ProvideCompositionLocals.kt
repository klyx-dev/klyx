package com.klyx.ui.provider

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.klyx.core.ProvideWindowAdaptiveInfo
import com.klyx.navigation.LocalNavigator
import com.klyx.navigation.rememberNavigator

@Composable
fun ProvideCompositionLocals(content: @Composable () -> Unit) {
    ProvideWindowAdaptiveInfo {
        val navigator = rememberNavigator()

        CompositionLocalProvider(
            LocalNavigator provides navigator,
            content = content
        )
    }
}
