package com.klyx.core

import arrow.core.None
import arrow.core.Option
import arrow.core.some
import kotlinx.io.files.Path
import platform.Foundation.NSHomeDirectory

@Suppress("ClassName")
actual object dirs {

    private fun pathOf(s: String?): Option<Path> =
        if (s == null || s.isBlank()) None else Path(s).some()

    private val home: String
        get() = NSHomeDirectory()

    // ~/Library
    private val library: String
        get() = "$home/Library"

    // ~/Library/Application Support
    private val appSupport: String
        get() = "$library/Application Support"

    // ~/Library/Preferences
    private val preferences: String
        get() = "$library/Preferences"

    // ~/Library/Caches
    private val caches: String
        get() = "$library/Caches"

    // ~/Documents
    private val documents: String
        get() = "$home/Documents"

    actual val homeDir: Option<Path> by lazy {
        pathOf(home)
    }

    actual val cacheDir: Option<Path> by lazy {
        pathOf(caches)
    }

    actual val configDir: Option<Path> by lazy {
        pathOf(appSupport)
    }

    actual val configLocalDir: Option<Path> by lazy {
        pathOf(appSupport)
    }

    actual val dataDir: Option<Path> by lazy {
        pathOf(appSupport)
    }

    actual val dataLocalDir: Option<Path> by lazy {
        pathOf(appSupport)
    }

    // iOS cannot run or write executables; system dirs are blocked
    actual val executableDir: Option<Path> = None

    actual val preferenceDir: Option<Path> by lazy {
        pathOf(preferences)
    }

    actual val runtimeDir: Option<Path> by lazy {
        pathOf("$home/tmp")
    }

    actual val stateDir: Option<Path> by lazy {
        pathOf("$appSupport/state")
    }

    // iOS does not expose user music/picture/video dirs
    actual val audioDir: Option<Path> = None
    actual val desktopDir: Option<Path> = None

    actual val documentDir: Option<Path> by lazy {
        pathOf(documents)
    }

    actual val downloadDir: Option<Path> = None

    // (read-only)
    actual val fontDir: Option<Path> by lazy {
        pathOf("$library/Fonts")
    }

    actual val pictureDir: Option<Path> = None
    actual val publicDir: Option<Path> = None
    actual val templateDir: Option<Path> = None
    actual val videoDir: Option<Path> = None
}
