package com.klyx.core.io

import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.withAndroidContext
import kotlinx.io.files.Path

actual object Paths {
    actual val dataDir by lazy {
        withAndroidContext { dataDir.toKotlinxIoPath() }
    }

    actual val configDir by lazy {
        withAndroidContext { getDir("config", 0).toKotlinxIoPath() }
    }

    actual val tempDir by lazy {
        withAndroidContext { cacheDir.toKotlinxIoPath() }
    }

    actual val logsDir by lazy {
        withAndroidContext { getDir("logs", 0).toKotlinxIoPath() }
    }
}

actual val Paths.androidExternalFilesDir
    get() = withAndroidContext {
        getExternalFilesDir(null)
            ?.absolutePath
            ?.let(::Path)
            ?: error("shared storage is not currently available.")
    }

actual val Paths.androidNativeLibraryDir: Path
    get() = withAndroidContext { applicationInfo.nativeLibraryDir.let(::Path) }
