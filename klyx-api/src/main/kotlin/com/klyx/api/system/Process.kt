package com.klyx.api.system

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.util.Log

fun Process.pid(): Int {
    return try {
        val field = javaClass.getDeclaredField("pid") // from java.lang.UNIXProcess
        field.isAccessible = true
        try {
            field.getInt(this)
        } finally {
            field.isAccessible = false
        }
    } catch (_: Exception) {
        -1
    }
}

fun Process.terminate() {
    val pid = pid()
    try {
        Os.kill(pid, OsConstants.SIGTERM)
    } catch (e: ErrnoException) {
        Log.w("Klyx", "Failed to send SIGTERM to Process with pid $pid: ${e.message}")
    }
}

fun Process.kill() {
    val pid = pid()
    try {
        Os.kill(pid, OsConstants.SIGKILL)
    } catch (e: ErrnoException) {
        Log.w("Klyx", "Failed to send SIGKILL to Process with pid $pid: ${e.message}")
    }
}
