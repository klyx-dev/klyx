package com.klyx.core.app

import android.util.Log
import com.klyx.core.process.Thread
import com.klyx.core.process.wrap

actual fun setupCrashHandler(onCrash: (Thread, Throwable) -> Unit) {
    val previousHandler = java.lang.Thread.getDefaultUncaughtExceptionHandler()

    java.lang.Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            onCrash(thread.wrap(), throwable)
            Log.e("CrashHandler", "Uncaught exception in thread ${thread.name}", throwable)
        } catch (t: Throwable) {
            Log.e("CrashHandler", "Error inside CrashHandler", t)
        } finally {
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
