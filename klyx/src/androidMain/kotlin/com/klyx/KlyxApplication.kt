package com.klyx

import android.content.Intent
import android.os.Build
import android.os.StrictMode
import android.os.strictmode.DiskReadViolation
import android.os.strictmode.UntaggedSocketViolation
import android.util.Log
import android.widget.Toast
import com.blankj.utilcode.util.FileUtils
import com.klyx.activities.CrashActivity
import com.klyx.core.KlyxBuildConfig
import com.klyx.core.app.Application
import com.klyx.core.app.CrashReport
import com.klyx.core.app.setupCrashHandler
import com.klyx.core.di.initKoin
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
import com.klyx.core.initializeKlyx
import com.klyx.core.io.Paths
import com.klyx.core.io.logFile
import com.klyx.core.logging.Level
import com.klyx.core.logging.LoggerConfig
import com.klyx.core.process.Thread
import com.klyx.core.terminal.klyxBinDir
import com.klyx.core.terminal.sandboxDir
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
import kotlinx.coroutines.withContext
import org.eclipse.tm4e.core.registry.IThemeSource
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import java.util.concurrent.Executors
import kotlin.time.ExperimentalTime

@OptIn(DelicateCoroutinesApi::class)
class KlyxApplication : android.app.Application(), CoroutineScope by GlobalScope {
    companion object {
        private lateinit var instance: KlyxApplication
        val application: KlyxApplication get() = instance
    }

    private val themeRegistry = ThemeRegistry.getInstance()

    /**
     * Klyx app instance
     */
    private lateinit var app: Application

    @Suppress("KotlinConstantConditions")
    override fun onCreate() {
        super.onCreate()
        instance = this
        System.setProperty("kotlin-logging-to-android-native", "true")
        setupCrashHandler(::handleUncaughtException)

        initKoin(commonModule) {
            androidLogger()
            androidContext(this@KlyxApplication)
        }

        if (KlyxBuildConfig.ENABLE_STRICT_MODE) {
            setupStrictModePolicies()
        }

        if (!KlyxBuildConfig.IS_DEBUG) {
            LoggerConfig.Default = LoggerConfig(replayBufferSize = 500)
        }

        app = Application()
        launch { initializeKlyx(app) }

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

    private fun setupStrictModePolicies() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                            if (!violation.stackTrace.any {
                                    it.className.contains("com.blankj.utilcode.util") ||
                                            it.className.contains("com.oplus.uifirst") ||
                                            it.className.contains("com.sun.jna")
                                }
                            ) {
                                if (violation is DiskReadViolation &&
                                    violation.stackTrace.any {
                                        it.className == "java.lang.System" && it.methodName == "loadLibrary"
                                    }
                                ) {
                                    return@penaltyListener
                                }

                                Log.e("Klyx", "StrictMode ThreadPolicy violation", violation)
                            }
                        }
                    } else {
                        penaltyLog()
                    }
                }
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder().apply {
                detectAll()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                        if (violation !is UntaggedSocketViolation) {
                            Log.e("Klyx", "StrictMode VmPolicy violation: $violation", violation)
                        }
                    }
                } else {
                    penaltyLog()
                }
            }.build()
        )
    }

    private fun setupTerminalFiles() {
        listOf("init", "sandbox", "setup", "utils", "universal_runner").forEach { setupAssetFile(it) }
    }

    private fun setupAssetFile(fileName: String) {
        with(klyxBinDir.resolve(fileName)) {
            parentFile?.mkdirs()
            writeBytes(assets.open("terminal/$fileName.sh").use { it.readBytes() })
            setExecutable(true)
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        app.app.shutdown()
    }
}

@JvmSynthetic
private fun KlyxApplication.handleUncaughtException(thread: Thread, throwable: Throwable) {
    launch(Dispatchers.IO) {
        val report = CrashReport(thread, throwable)
        val file = runCatching { saveCrashReport(report) }.getOrNull()
        EventBus.INSTANCE.tryPost(CrashEvent(thread, throwable, file))

        if (thread.name == "main") {
            withContext(Dispatchers.Main.immediate) {
                Toast.makeText(
                    this@handleUncaughtException,
                    "App Crashed. ${if (file != null) "A crash report was saved." else "Failed to save crash report."}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        Log.e("Klyx", report.toString())

        withContext(Dispatchers.Main) {
            startActivity(Intent(this@handleUncaughtException, CrashActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(CrashActivity.EXTRA_CRASH_LOG, report.toString())
            })
        }
    }
}

@OptIn(ExperimentalTime::class)
private suspend fun saveCrashReport(report: CrashReport): KxFile? = withContext(Dispatchers.IO) {
    val logFile = Paths.logFile.toKxFile()
    val externalLogFile = android.os.Environment.getExternalStorageDirectory().resolve("klyx/Logs/Klyx.log")
    FileUtils.createFileByDeleteOldFile(externalLogFile)

    if (logFile.createNewFile()) {
        val logString = report.toString()
        logFile.writeText(logString)
        externalLogFile.writeText(logString)
        logFile
    } else {
        Log.e("Klyx", "Failed to save crash logs")
        null
    }
}
