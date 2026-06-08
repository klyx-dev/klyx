package com.klyx

import android.app.Application
import com.klyx.core.App
import com.klyx.core.initApp
import com.klyx.data.fs.Paths
import com.klyx.data.terminal.SessionBinder
import com.klyx.event.initializeGlobalEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.core.logger.Level
import org.koin.plugin.module.dsl.startKoin

@OptIn(DelicateCoroutinesApi::class)
@KoinApplication
class KlyxApplication : Application(), CoroutineScope by GlobalScope {

    lateinit var app: App
        private set

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("klyx")

        startKoin<KlyxApplication> {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.INFO)
            androidContext(this@KlyxApplication)
        }

        app = initApp()
        initializeGlobals()

        launch {
            withContext(Dispatchers.IO) {
                setupTerminalFiles()
            }
        }
    }

    private fun initializeGlobals() {
        initializeGlobalEventBus(app)
        app.setGlobal(SessionBinder())
    }

    private fun setupTerminalFiles() {
        listOf(
            "init",
            "sandbox",
            "setup",
            "utils",
            "universal_runner"
        ).forEach { fileName ->
            with(Paths.filesDir.resolve("usr/bin/$fileName")) {
                parentFile?.mkdirs()
                writeBytes(assets.open("terminal/$fileName.sh").use { it.readBytes() })
                setExecutable(true)
            }
        }
    }
}
