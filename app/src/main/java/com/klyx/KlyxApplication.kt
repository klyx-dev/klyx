package com.klyx

import android.app.Application
import com.klyx.core.App
import com.klyx.core.initApp
import com.klyx.data.terminal.DefaultTerminalSessionManager
import com.klyx.data.terminal.TerminalSessionBinder
import com.klyx.data.terminal.TerminalSessionBinderImpl
import com.klyx.data.terminal.TerminalSessionManager
import com.klyx.event.initializeGlobalEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class KlyxApplication : Application() {

    lateinit var app: App
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        System.loadLibrary("klyx")

        startKoin<KlyxApplication> {
            androidLogger()
            androidContext(this@KlyxApplication)
        }

        app = initApp()
        initializeGlobals()
    }

    private fun initializeGlobals() {
        initializeGlobalEventBus(app)
        app.setGlobal<TerminalSessionBinder>(TerminalSessionBinderImpl())
        app.setGlobal<TerminalSessionManager>(DefaultTerminalSessionManager())
    }
}
