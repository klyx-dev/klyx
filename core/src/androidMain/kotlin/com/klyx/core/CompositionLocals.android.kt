package com.klyx.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.blankj.utilcode.util.AppUtils

@Composable
actual fun PlatformLocalProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBuildVariant provides if (AppUtils.isAppDebug()) BuildVariant.Debug else BuildVariant.Release
    ) {
        content()
    }
}
