package com.klyx.core

import androidx.compose.runtime.staticCompositionLocalOf

actual abstract class PlatformContext private constructor() {
    companion object {
        @JvmField
        val INSTANCE = object : PlatformContext() {}
    }
}

actual val LocalPlatformContext = staticCompositionLocalOf { PlatformContext.INSTANCE }
