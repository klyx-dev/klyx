package com.klyx

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.blankj.utilcode.util.FileUtils
import com.klyx.activities.CrashActivity
import com.klyx.core.App
import com.klyx.core.Environment
import com.klyx.core.defaultLogsFile
import com.klyx.core.di.initKoin
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.file.KxFile
import com.klyx.core.file.rawFile
import com.klyx.core.file.toKxFile
import com.klyx.core.logging.Level
import com.klyx.core.logging.LoggerConfig
import com.klyx.core.terminal.klyxBinDir
import com.klyx.core.terminal.localDir
import com.klyx.core.terminal.sandboxDir
import com.klyx.di.commonModule
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.tm4e.core.registry.IThemeSource
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import java.io.File
import java.io.FileOutputStream
import java.io.PrintStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(DelicateCoroutinesApi::class)
class KlyxApplication : Application(), CoroutineScope by GlobalScope {
    companion object {
        private lateinit var instance: KlyxApplication
        val application: KlyxApplication get() = instance
    }

    private val themeRegistry = ThemeRegistry.getInstance()

    @Suppress("KotlinConstantConditions")
    override fun onCreate() {
        super.onCreate()
        instance = this
        Thread.setDefaultUncaughtExceptionHandler(::handleUncaughtException)
        initKoin(commonModule) {
            androidLogger()
            androidContext(this@KlyxApplication)
        }

        launch { App.init() }

        try {
            Environment.init()
        } catch (e: Exception) {
            Log.e("Klyx", "Failed to initialize environment", e)
            handleUncaughtException(Thread.currentThread(), e)
        }

        launch {
            Environment.defaultLogsFile().delete()
            //redirectPrintlnToFile(Environment.defaultLogsFile())
        }

        @Suppress("SimplifyBooleanWithConstants")
        if (BuildConfig.BUILD_TYPE == "release") {
            LoggerConfig.Default = LoggerConfig(
                minimumLevel = Level.Info
            )
        }

        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(assets)
        )

        launch {
            GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")

            val themes = listOf("darcula", "quietlight")
            for (theme in themes) {
                val path = "textmate/$theme.json"

                themeRegistry.loadTheme(
                    ThemeModel(
                        IThemeSource.fromInputStream(
                            FileProviderRegistry.getInstance().tryGetInputStream(path), path, null
                        ),
                        theme
                    )
                )
            }
        }

        launch {
            runCatching {
                setupTerminalFiles()
            }.onFailure { exception ->
                Log.e("Klyx", "Failed to setup terminal files", exception)
            }
        }
    }

    private fun setupTerminalFiles() {
        listOf("init", "sandbox", "setup", "utils", "universal_runner").forEach { setupAssetFile(it) }
        with(filesDir.resolve("ubuntu")) {
            if (exists()) {
                renameTo(sandboxDir)
                localDir.resolve(".terminal_setup_ok_DO_NOT_REMOVE").createNewFile()
            }
        }
    }

    private fun setupAssetFile(fileName: String) {
        with(klyxBinDir.resolve(fileName)) {
            parentFile?.mkdirs()
            writeBytes(assets.open("terminal/$fileName.sh").use { it.readBytes() })
        }
    }
}

@JvmSynthetic
private fun KlyxApplication.handleUncaughtException(thread: Thread, throwable: Throwable) {
    if (throwable is ResponseErrorException &&
        (throwable.message?.contains("content modified") == true ||
                throwable.message?.contains("server cancelled") == true)
    ) {
        return
    }

    val file = runCatching { saveLogs(thread, throwable) }.getOrNull()
    EventBus.INSTANCE.postSync(CrashEvent(thread, throwable, file))

    if (thread.name == "main") {
        Toast.makeText(
            this,
            "App Crashed. ${if (file != null) "A crash report was saved." else "Failed to save crash report."}",
            Toast.LENGTH_LONG
        ).show()
    }

    Log.e("Klyx", file?.readText() ?: buildLogString(thread, throwable))

    startActivity(Intent(this, CrashActivity::class.java).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(CrashActivity.EXTRA_CRASH_LOG, file?.readText() ?: buildLogString(thread, throwable))
    })
}

@OptIn(ExperimentalTime::class)
private fun saveLogs(thread: Thread, throwable: Throwable): KxFile? {
    val logFile = File(Environment.LogsDir, "log_${Clock.System.now().toEpochMilliseconds()}.txt")
    val externalLogFile = android.os.Environment.getExternalStorageDirectory().resolve("klyx/Logs/Klyx.log")
    FileUtils.createFileByDeleteOldFile(externalLogFile)

    return if (logFile.createNewFile()) {
        val logString = buildLogString(thread, throwable)
        logFile.writeText(logString)
        externalLogFile.writeText(logString)
        logFile.toKxFile()
    } else {
        Log.e("Klyx", "Failed to save crash logs")
        null
    }
}

private fun buildLogString(thread: Thread, throwable: Throwable): String = buildString {
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

@JvmSynthetic
private fun redirectPrintlnToFile(logFile: KxFile) {
    val originalOut = System.out
    val timestampFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    val fileStream = FileOutputStream(logFile.rawFile(), true)
    val printStream = object : PrintStream(fileStream, true) {
        override fun println(x: Any?) {
            val timestamp = timestampFormat.format(Date())
            val line = "[$timestamp] $x"
            super.println(line)
            originalOut.println(line)
        }
    }

    System.setOut(printStream)
    System.setErr(printStream)
}
