package com.klyx

import android.app.Application
import android.os.Process
import android.util.Log
import android.widget.Toast
import com.klyx.core.Environment
import com.klyx.core.di.initKoin
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.extension.ExtensionFactory
import com.klyx.viewmodel.EditorViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import java.io.File
import kotlin.system.exitProcess
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

        initKoin(module {
            viewModelOf(::EditorViewModel)
            single { ExtensionFactory.create(get()) }
        }) {
            androidLogger()
            androidContext(this@KlyxApplication)
        }
    }
}

private fun KlyxApplication.handleUncaughtException(thread: Thread, throwable: Throwable) {
    val file = saveLogs(thread, throwable)

    if (thread.name == "main") {
        Toast.makeText(
            this,
            "App Crashed. ${if (file != null) "A crash report was saved." else "Failed to save crash report."}",
            Toast.LENGTH_LONG
        ).show()

        Process.sendSignal(Process.myPid(), Process.SIGNAL_KILL)
        exitProcess(Process.SIGNAL_KILL)
    }

    EventBus.instance.postSync(CrashEvent(thread, throwable, file))
}

@OptIn(ExperimentalTime::class)
private fun saveLogs(thread: Thread, throwable: Throwable): KxFile? {
    val logFile = File(Environment.LogsDir, "log_${Clock.System.now().toEpochMilliseconds()}.txt")
    return if (logFile.createNewFile()) {
        val logString = buildString {
            appendLine("=== Crash Log ===")
            appendLine("Time: ${Clock.System.now()}")
            appendLine("Thread: ${thread.name} (id=${thread.id})")
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
