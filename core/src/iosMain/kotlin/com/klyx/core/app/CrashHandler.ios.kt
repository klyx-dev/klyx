package com.klyx.core.app

import com.klyx.core.process.Thread
import platform.Foundation.NSLog
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
actual fun setupCrashHandler(onCrash: (Thread, Throwable) -> Unit) {
    setUnhandledExceptionHook { throwable ->
        try {
            onCrash(Thread.currentThread(), throwable)
            NSLog("CRASH: Uncaught Kotlin exception: ${throwable.message}")
            throwable.printStackTrace()
        } catch (t: Throwable) {
            NSLog("Failed to handle crash: ${t.message}")
        } finally {
            // CRITICAL: We must terminate the process.
            // If we don't, the runtime state is undefined and the app might hang.
            // This function effectively crashes the app "cleanly" from Kotlin's perspective.
            terminateWithUnhandledException(throwable)
        }
    }
}
