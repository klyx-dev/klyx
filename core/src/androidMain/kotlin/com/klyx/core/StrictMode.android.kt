package com.klyx.core

import android.os.StrictMode
import androidx.annotation.MainThread

/**
 * Temporarily relaxes StrictMode to allow disk reads on the current thread.
 *
 * This should be used only for small, fast I/O operations that may legitimately
 * occur on the main thread, such as SharedPreferences access during initialization.
 *
 * @param block The operation that performs disk reads.
 * @return The result of the block.
 */
@MainThread
actual inline fun <T> allowDiskReads(block: () -> T): T {
    if (KlyxBuildConfig.ENABLE_STRICT_MODE) {
        val old = StrictMode.allowThreadDiskReads()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(old)
        }
    } else {
        return block()
    }
}

/**
 * Temporarily relaxes StrictMode to allow disk writes on the current thread.
 *
 * This should be used sparingly and only for small or unavoidable synchronous
 * disk writes. Prefer using background threads for larger or potentially slow I/O.
 *
 * @param block The operation that performs disk writes.
 * @return The result of the block.
 */
@MainThread
actual inline fun <T> allowDiskWrites(block: () -> T): T {
    if (KlyxBuildConfig.ENABLE_STRICT_MODE) {
        val old = StrictMode.allowThreadDiskWrites()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(old)
        }
    } else {
        return block()
    }
}

/**
 * Temporarily relaxes StrictMode to allow both disk reads and writes on the current thread.
 *
 * This should be used sparingly and only for operations that require both read and write
 * access to disk. Prefer using background threads for larger or potentially slow I/O.
 *
 * @param block The operation that performs disk reads and writes.
 * @return The result of the block.
 */
@MainThread
actual inline fun <T> allowDiskReadsWrites(block: () -> T): T {
    if (KlyxBuildConfig.ENABLE_STRICT_MODE) {
        val old = StrictMode.allowThreadDiskWrites()
        StrictMode.allowThreadDiskReads()
        return try {
            block()
        } finally {
            StrictMode.setThreadPolicy(old)
        }
    } else {
        return block()
    }
}
