package com.klyx

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.itsaky.androidide.treesitter.TreeSitter
import com.klyx.core.Environment
import com.klyx.core.di.initKoin
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.viewmodel.EditorViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.io.File
import java.util.Date
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class KlyxApplication : Application() {
    companion object {
        private lateinit var instance: KlyxApplication
        val application: KlyxApplication get() = instance
    }

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(::handleUncaughtException)
        instance = this
        TreeSitter.loadLibrary()

        initKoin(module {
            viewModelOf(::EditorViewModel)
        }) {
            androidLogger()
            androidContext(this@KlyxApplication)
        }
    }
}

private fun KlyxApplication.handleUncaughtException(thread: Thread, throwable: Throwable) {
    val file = saveLogs(thread, throwable)
    EventBus.instance.postSync(CrashEvent(thread, throwable, file))

    if (thread.name == "main") {
        Toast.makeText(
            this,
            "App Crashed. ${if (file != null) "A crash report was saved." else "Failed to save crash report."}",
            Toast.LENGTH_LONG
        ).show()

        System.err.println(file?.readText())
    }

    startActivity(Intent(this, CrashActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(CrashActivity.EXTRA_CRASH_LOG, file?.readText())
    })
}

@OptIn(ExperimentalTime::class)
private fun saveLogs(thread: Thread, throwable: Throwable): KxFile? {
    val logFile = File(Environment.LogsDir, "log_${Clock.System.now().toEpochMilliseconds()}.txt")
    return if (logFile.createNewFile()) {
        val logString = buildString {
            appendLine("=== Crash Log ===")
            appendLine("Time: ${Date()}")

            val id = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
                thread.threadId()
            } else {
                @Suppress("DEPRECATION")
                thread.id
            }

            appendLine("Thread: ${thread.name} (id=$id)")
            appendLine("Exception: ${throwable::class.qualifiedName}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine("Stack Trace:")
            throwable.stackTrace.forEach { trace ->
                appendLine("\tat $trace")
            }
        }
        logFile.writeText(logString)
        logFile.toKxFile()
    } else {
        Log.e("Klyx", "Failed to save crash logs")
        null
    }
}
