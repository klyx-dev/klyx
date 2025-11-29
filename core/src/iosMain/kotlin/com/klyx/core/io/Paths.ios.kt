package com.klyx.core.io

import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.io.files.Path
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSLibraryDirectory
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUserDomainMask

actual object Paths {
    private fun pathOf(dir: NSSearchPathDirectory): Path {
        val paths = NSSearchPathForDirectoriesInDomains(dir, NSUserDomainMask, true)
        val first = paths.firstOrNull() as? String
            ?: error("Unable to resolve iOS directory: $dir")
        return Path(first)
    }

    actual val dataDir: Path by lazy {
        val p = pathOf(NSApplicationSupportDirectory).join("Klyx")
        ensureDirectoryExists(p)
        p
    }

    actual val configDir: Path by lazy {
        val p = pathOf(NSLibraryDirectory).join("Preferences", "Klyx")
        ensureDirectoryExists(p)
        p
    }

    actual val tempDir: Path by lazy {
        val p = Path(NSTemporaryDirectory()).join("Klyx")
        ensureDirectoryExists(p)
        p
    }

    actual val logsDir: Path by lazy {
        val p = pathOf(NSLibraryDirectory).join("Logs", "Klyx")
        ensureDirectoryExists(p)
        p
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun ensureDirectoryExists(path: Path) {
        val fm = NSFileManager.defaultManager()
        val isDir = memScoped {
            val flag = alloc<BooleanVar>()
            fm.fileExistsAtPath(path.toString(), flag.ptr)
            flag.value
        }

        if (!isDir) {
            fm.createDirectoryAtPath(
                path = path.toString(),
                withIntermediateDirectories = true,
                attributes = null,
                error = null
            )
        }
    }
}
