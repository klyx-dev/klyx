package com.klyx.core.io

import com.klyx.core.file.toKotlinxIoPath
import com.klyx.core.withAndroidContext

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
