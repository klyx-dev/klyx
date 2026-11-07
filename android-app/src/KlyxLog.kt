package com.klyx

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object KlyxLog {
    private const val MAX_BYTES = 256 * 1024
    private const val MAX_FILES = 3
    private val lock = Any()

    private var logDir: File? = null

    fun init(context: Context) {
        logDir = File(context.applicationContext.filesDir, "logs").apply { mkdirs() }
    }

    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val dir = logDir ?: return

        val timestamp = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
        val line = buildString {
            append(timestamp)
            append(' ')
            append(level)
            append('/')
            append(tag)
            append(": ")
            append(message)
            if (throwable != null) {
                append('\n')
                append(throwable.stackTraceToString())
            }
            append('\n')
        }

        synchronized(lock) {
            try {
                val file = File(dir, "klyx.log")
                if (file.length() > MAX_BYTES) rotate(dir)
                file.appendText(line)
            } catch (t: Throwable) {
                Log.e("KlyxLog", "Failed to write log", t)
            }
        }
    }

    fun recentLines(limit: Int = 200): String = synchronized(lock) {
        val dir = logDir
        if (dir == null) "" else {
            val files = (MAX_FILES - 1 downTo 0).map { index ->
                File(dir, if (index == 0) "klyx.log" else "klyx.log.$index")
            }.filter { it.exists() }

            files.flatMap { file ->
                runCatching { file.readLines() }.getOrDefault(emptyList())
            }.takeLast(limit).joinToString("\n")
        }
    }

    private fun rotate(dir: File) {
        for (index in MAX_FILES - 1 downTo 1) {
            val from = File(dir, if (index == 1) "klyx.log" else "klyx.log.${index - 1}")
            val to = File(dir, "klyx.log.$index")
            if (to.exists()) to.delete()
            if (from.exists()) from.renameTo(to)
        }
    }
}
