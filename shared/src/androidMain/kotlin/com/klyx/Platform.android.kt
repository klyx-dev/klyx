package com.klyx

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val os: String = System.getProperty("os.name") ?: "Android"
}

actual fun platform(): Platform = AndroidPlatform()
