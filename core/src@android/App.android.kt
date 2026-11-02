package com.klyx.core

import android.content.Context
import android.content.pm.ApplicationInfo

actual fun App.isDebugMode(): Boolean {
    val context: Context = get()
    return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}

/**
 * Android application context.
 */
val App.applicationContext: Context get() = get()
