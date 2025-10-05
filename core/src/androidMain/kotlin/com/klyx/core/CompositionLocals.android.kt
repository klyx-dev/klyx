package com.klyx.core

import android.content.SharedPreferences
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.blankj.utilcode.util.AppUtils

@Composable
actual fun PlatformLocalProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("klyx", 0) }

    CompositionLocalProvider(
        LocalSharedPreferences provides prefs,
        LocalBuildVariant provides if (AppUtils.isAppDebug()) BuildVariant.Debug else BuildVariant.Release,
    ) {
        content()
    }
}

val LocalSharedPreferences = staticCompositionLocalOf<SharedPreferences> {
    noLocalProvidedFor<SharedPreferences>()
}

@Composable
actual fun dynamicDarkColorScheme(): ColorScheme {
    return if (Build.VERSION.SDK_INT >= 31) {
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        darkColorScheme()
    }
}
