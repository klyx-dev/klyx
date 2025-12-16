package com.klyx.core.app

import com.klyx.core.process.Thread
import com.klyx.core.process.wrap
import kotlin.system.exitProcess

actual fun setupCrashHandler(onCrash: (Thread, Throwable) -> Unit) {
    val previousHandler = java.lang.Thread.getDefaultUncaughtExceptionHandler()

    java.lang.Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
        try {
            onCrash(thread.wrap(), throwable)
            System.err.println("CRASH: Uncaught exception in thread ${thread.name}")
            throwable.printStackTrace()
        } catch (t: Throwable) {
            System.err.println("Error inside CrashHandler")
            t.printStackTrace()
        } finally {
            if (previousHandler != null) {
                previousHandler.uncaughtException(thread, throwable)
            } else {
                exitProcess(1)
            }
        }
    }
}
