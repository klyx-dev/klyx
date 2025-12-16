package com.klyx

import android.app.Application
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
import com.klyx.core.app.App
import com.klyx.core.app.CrashReport
import com.klyx.core.app.setupCrashHandler
import com.klyx.core.di.initKoin
import com.klyx.core.event.CrashEvent
import com.klyx.core.event.EventBus
import com.klyx.core.file.KxFile
import com.klyx.core.file.toKxFile
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
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException
import org.eclipse.tm4e.core.registry.IThemeSource
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
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
        setupCrashHandler(::handleUncaughtException)

        if (KlyxBuildConfig.ENABLE_STRICT_MODE) {
            setupStrictModePolicies()
        }

        initKoin(commonModule) {
            androidLogger()
            androidContext(this@KlyxApplication)
        }

        launch { App.init() }

        if (!KlyxBuildConfig.IS_DEBUG) {
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

    private fun setupStrictModePolicies() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        penaltyListener(Executors.newSingleThreadExecutor()) { violation ->
                            if (!violation.stackTrace.any {
                                    it.className.startsWith("com.blankj.utilcode.util") ||
                                            it.className.startsWith("com.oplus.uifirst")
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
                            Log.e("Klyx", "StrictMode VmPolicy violation", violation)
                        }
                    }
                }

                penaltyLog()
            }.build()
        )
    }

    private fun setupTerminalFiles() {
        listOf("init", "sandbox", "setup", "utils", "universal_runner").forEach { setupAssetFile(it) }
        with(filesDir.resolve("ubuntu")) {
            if (exists()) {
                renameTo(sandboxDir)
                dataDir.resolve(".terminal_setup_ok_DO_NOT_REMOVE").createNewFile()
            }
        }
    }

    private fun setupAssetFile(fileName: String) {
        with(klyxBinDir.resolve(fileName)) {
            parentFile?.mkdirs()
            writeBytes(assets.open("terminal/$fileName.sh").use { it.readBytes() })
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        App.shutdown(reason = "Application terminated.")
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

    MainScope().launch(Dispatchers.IO) {
        val report = CrashReport(thread, throwable)
        val file = runCatching { saveCrashReport(report) }.getOrNull()
        EventBus.INSTANCE.postSync(CrashEvent(thread, throwable, file))

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

    App.shutdown("Crash")
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
