@file:Suppress("MatchingDeclarationName")

package com.klyx

import android.os.Build
import java.io.File

class AndroidPlatform : Platform {
    override val name = "Android ${Build.VERSION.SDK_INT}"
    override val os = System.getProperty("os.name") ?: "Android"
    override val architecture = System.getProperty("os.arch") ?: "unknown"
}

actual fun platform(): Platform = AndroidPlatform()
actual val fileSeparatorChar: Char get() = File.separatorChar
actual val lineSeparator: String get() = System.lineSeparator()
