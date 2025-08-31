package com.klyx

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import com.itsaky.androidide.treesitter.TreeSitter
import com.klyx.activities.CrashActivity
import com.klyx.core.Environment
import com.klyx.core.di.initKoin
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.core.logging.Level
import com.klyx.core.logging.LoggerConfig
import com.klyx.di.commonModule
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.tm4e.core.registry.IThemeSource
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import java.io.File
import java.util.Date
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
        Thread.setDefaultUncaughtExceptionHandler(::handleUncaughtException)
        instance = this
        TreeSitter.loadLibrary()

        if (BuildConfig.BUILD_TYPE == "release") {
            LoggerConfig.Default = LoggerConfig(
                minimumLevel = Level.Info
            )
        }

        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(assets)
        )

        launch(Dispatchers.IO) {
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

        initKoin(commonModule) {
            androidLogger()
            androidContext(this@KlyxApplication)
        }
    }
}

private fun KlyxApplication.handleUncaughtException(thread: Thread, throwable: Throwable) {
    if (throwable is ResponseErrorException &&
        (throwable.message?.contains("content modified") == true ||
                throwable.message?.contains("server cancelled") == true)
    ) {
        return
    }

    val file = saveLogs(thread, throwable)
    EventBus.instance.postSync(CrashEvent(thread, throwable, file))

    if (thread.name == "main") {
        Toast.makeText(
            this,
            "App Crashed. ${if (file != null) "A crash report was saved." else "Failed to save crash report."}",
            Toast.LENGTH_LONG
        ).show()
    }

    Log.e("Klyx", file?.readText() ?: "App crashed. No crash logs.")

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
