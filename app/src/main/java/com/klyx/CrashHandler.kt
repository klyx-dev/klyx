package com.klyx

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CrashHandler {

    private const val FILE_NAME = "last_crash.json"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null

    var currentTabId: String? = null
    var currentPluginId: String? = null

    fun install(context: Context) {
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            saveCrash(context, throwable)
            defaultHandler?.uncaughtException(thread, throwable)
                ?: Process.killProcess(Process.myPid())
        }
    }

    fun hasSavedCrash(context: Context): Boolean {
        return crashFile(context).exists()
    }

    fun loadCrash(context: Context): CrashData? {
        val file = crashFile(context)
        if (!file.exists()) return null
        return try {
            val text = file.readText()
            val parts = text.split("\n---END---\n", limit = 6)
            val hasPluginId = parts.size >= 6
            val hasTabId = parts.size >= 5
            if (parts.size >= 4) {
                CrashData(
                    timestamp = parts[0],
                    exceptionClass = parts[1],
                    message = parts[2],
                    crashedTabId = if (hasTabId) parts[3].takeIf { it.isNotBlank() } else null,
                    crashedPluginId = if (hasPluginId) parts[4].takeIf { it.isNotBlank() } else null,
                    stackTrace = if (hasPluginId) parts[5] else if (hasTabId) parts[4] else parts[3]
                )
            } else null
        } catch (_: Exception) {
            null
        } finally {
            file.delete()
        }
    }

    fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    private fun saveCrash(context: Context, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            pw.flush()
            val stackTrace = sw.toString()
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val exceptionClass = throwable.javaClass.name
            val message = throwable.message ?: "No message"
            val content = buildString {
                appendLine(timestamp)
                appendLine("---END---")
                appendLine(exceptionClass)
                appendLine("---END---")
                appendLine(message)
                appendLine("---END---")
                appendLine(currentTabId ?: "")
                appendLine("---END---")
                appendLine(currentPluginId ?: "")
                appendLine("---END---")
                append(stackTrace)
            }
            crashFile(context).writeText(content)
            showCrashToast(context)
        } catch (e: Exception) {
            Log.e("CrashHandler", "Failed to save crash report", e)
        }
    }

    private fun showCrashToast(context: Context) {
        try {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, "App crashed. Crash report saved.", Toast.LENGTH_LONG).show()
            }
        } catch (_: Exception) {
        }
    }

    private fun crashFile(context: Context): File {
        return File(context.filesDir, FILE_NAME)
    }

    data class CrashData(
        val timestamp: String,
        val exceptionClass: String,
        val message: String,
        val crashedTabId: String? = null,
        val crashedPluginId: String? = null,
        val stackTrace: String
    )
}
