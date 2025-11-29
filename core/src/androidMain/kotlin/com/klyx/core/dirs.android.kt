package com.klyx.core

import android.os.Environment
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import arrow.core.some
import kotlinx.io.files.Path
import java.io.File

@Suppress("ClassName")
actual object dirs {

    private fun pathOf(f: File?): Option<Path> =
        f?.let { Path(it.absolutePath).some() } ?: None

    private fun externalDir(type: String): Option<Path> =
        pathOf(Environment.getExternalStoragePublicDirectory(type))

    actual val homeDir: Option<Path> by lazy {
        pathOf(Environment.getExternalStorageDirectory())
    }

    actual val cacheDir: Option<Path> by lazy {
        pathOf(Environment.getDownloadCacheDirectory())
    }

    actual val configDir: Option<Path> = None

    actual val configLocalDir: Option<Path> = None

    actual val dataDir: Option<Path> by lazy {
        pathOf(Environment.getDataDirectory())
    }

    actual val dataLocalDir: Option<Path> = None

    actual val executableDir: Option<Path> by lazy {
        val sysBin = File("/system/bin")
        if (sysBin.exists()) Some(Path(sysBin.absolutePath)) else None
    }

    actual val preferenceDir: Option<Path> = None
    actual val runtimeDir: Option<Path> = None
    actual val stateDir: Option<Path> = None

    actual val audioDir: Option<Path> by lazy {
        externalDir(Environment.DIRECTORY_MUSIC)
    }

    actual val desktopDir: Option<Path> = None

    actual val documentDir: Option<Path> by lazy {
        externalDir(Environment.DIRECTORY_DOCUMENTS)
    }

    actual val downloadDir: Option<Path> by lazy {
        externalDir(Environment.DIRECTORY_DOWNLOADS)
    }

    actual val fontDir: Option<Path> = None

    actual val pictureDir: Option<Path> by lazy {
        externalDir(Environment.DIRECTORY_PICTURES)
    }

    actual val publicDir: Option<Path> = None
    actual val templateDir: Option<Path> = None

    actual val videoDir: Option<Path> by lazy {
        externalDir(Environment.DIRECTORY_MOVIES)
    }
}
