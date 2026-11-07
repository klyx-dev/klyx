package com.klyx

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import com.klyx.build.BuildInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

@Serializable
data class CrashReport(
    val timestamp: String,
    val kind: String,
    val versionName: String,
    val versionCode: Long,
    val commit: String,
    val branch: String,
    val device: String,
    val android: String,
    val thread: String,
    val exceptionClass: String,
    val message: String,
    val stackTrace: String,
    val logTail: String,
)

fun CrashReport.toShareText(): String = buildString {
    appendLine("Klyx crash report")
    appendLine("Time: $timestamp")
    appendLine("Kind: $kind")
    appendLine("Version: $versionName ($versionCode)")
    appendLine("Commit: $commit ($branch)")
    appendLine("Device: $device")
    appendLine("Android: $android")
    appendLine("Thread: $thread")
    appendLine("Exception: $exceptionClass")
    appendLine("Message: $message")
    appendLine()
    appendLine("Stack trace:")
    appendLine(stackTrace)
    if (logTail.isNotBlank()) {
        appendLine()
        appendLine("Recent logs:")
        appendLine(logTail)
    }
}

object CrashReporter {
    private const val DIR = "crashes"
    private const val FILE = "pending.json"

    fun install(context: Context) {
        val app = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                save(app, reportForJvmCrash(app, thread, throwable))
            } catch (_: Throwable) {
                //ignore
            }

            previous?.uncaughtException(thread, throwable) ?: run {
                Process.killProcess(Process.myPid())
                exitProcess(10)
            }
        }
    }

    fun capturePreviousExit(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return

        val app = context.applicationContext
        if (pendingFile(app).exists()) return

        try {
            val manager = app.getSystemService(ActivityManager::class.java) ?: return
            val reasons = manager.getHistoricalProcessExitReasons(app.packageName, 0, 5)

            for (info in reasons) {
                val kind = when (info.reason) {
                    ApplicationExitInfo.REASON_ANR -> "anr"
                    ApplicationExitInfo.REASON_CRASH_NATIVE -> "native_crash"
                    ApplicationExitInfo.REASON_CRASH -> "crash"
                    ApplicationExitInfo.REASON_LOW_MEMORY -> "low_memory"
                    else -> continue
                }

                val trace = readTrace(info)
                if (kind == "crash" && trace.isBlank()) return

                val report = baseReport(app).copy(
                    timestamp = formatTime(info.timestamp),
                    kind = kind,
                    thread = "system",
                    exceptionClass = "ApplicationExitInfo",
                    message = buildString {
                        append("reason=")
                        append(info.reason)
                        val description = info.description
                        if (!description.isNullOrBlank()) {
                            append(" (")
                            append(description)
                            append(')')
                        }
                    },
                    stackTrace = trace.ifBlank { "(no trace)" },
                )

                save(app, report)
                break
            }
        } catch (_: Throwable) {
            //ignore
        }
    }

    fun loadPending(context: Context): CrashReport? {
        val file = pendingFile(context)
        if (!file.exists()) return null

        return try {
            Json.decodeFromString(file.readText())
        } catch (_: Throwable) {
            file.delete()
            null
        }
    }

    fun clearPending(context: Context) {
        pendingFile(context).delete()
    }

    private fun reportForJvmCrash(
        context: Context,
        thread: Thread,
        throwable: Throwable
    ): CrashReport = baseReport(context).copy(
        timestamp = formatTime(System.currentTimeMillis()),
        kind = "crash",
        thread = thread.name,
        exceptionClass = throwable.javaClass.name,
        message = throwable.message ?: "(no message)",
        stackTrace = throwable.stackTraceToString(),
    )

    private fun baseReport(context: Context): CrashReport {
        val info = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()

        return CrashReport(
            timestamp = formatTime(System.currentTimeMillis()),
            kind = "crash",
            versionName = info?.versionName ?: "?",
            versionCode = info?.let(::packageVersionCode) ?: 0L,
            commit = BuildInfo.COMMIT,
            branch = BuildInfo.BRANCH,
            device = "${Build.MANUFACTURER} ${Build.MODEL}",
            android = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            thread = "main",
            exceptionClass = "",
            message = "",
            stackTrace = "",
            logTail = redact(KlyxLog.recentLines(200)),
        )
    }

    private fun packageVersionCode(info: PackageInfo) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }

    private fun save(context: Context, report: CrashReport) {
        val dir = File(context.filesDir, DIR).apply { mkdirs() }
        File(dir, FILE).writeText(Json.encodeToString(report))
    }

    private fun pendingFile(context: Context): File = File(File(context.filesDir, DIR), FILE)

    @RequiresApi(Build.VERSION_CODES.R)
    private fun readTrace(info: ApplicationExitInfo) = try {
        info.traceInputStream?.use { input ->
            val buffer = ByteArray(64 * 1024)
            val read = input.read(buffer)
            if (read > 0) String(buffer, 0, read) else ""
        } ?: ""
    } catch (_: Throwable) {
        ""
    }

    private fun formatTime(millis: Long): String =
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH).format(Date(millis))

    @SuppressLint("SdCardPath")
    private fun redact(value: String): String = value
        .replace(Regex("/storage/emulated/\\d+"), "<storage>")
        .replace(Regex("/sdcard"), "<storage>")
        .replace(Regex("/data/user/\\d+/[^/\\s]+"), "<app-data>")
}
