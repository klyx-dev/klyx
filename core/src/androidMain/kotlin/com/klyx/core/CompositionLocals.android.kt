package com.klyx.core

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import com.blankj.utilcode.util.AppUtils

@Composable
actual fun PlatformLocalProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalBuildVariant provides if (AppUtils.isAppDebug()) BuildVariant.Debug else BuildVariant.Release,
        content = content
    )
}

@Composable
actual fun dynamicDarkColorScheme(): ColorScheme {
    return if (Build.VERSION.SDK_INT >= 31) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        darkColorScheme()
    }
}

@Composable
actual fun dynamicLightColorScheme(): ColorScheme {
    return if (Build.VERSION.SDK_INT >= 31) {
        dynamicLightColorScheme(LocalContext.current)
    } else {
        lightColorScheme()
    }
}
