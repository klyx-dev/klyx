package com.klyx

import android.app.Application
import com.klyx.core.Log
import com.klyx.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class KlyxApp : Application() {
    override fun onCreate() {
        super.onCreate()
        KlyxLog.init(this)
        Log.sink = { level, tag, message, throwable ->
            KlyxLog.log(level, tag, message, throwable)
        }
        Log.i("KlyxApp", "onCreate")

        CrashReporter.install(this)
        CrashReporter.capturePreviousExit(this)

        initKoin {
            androidLogger()
            androidContext(this@KlyxApp)
        }
    }
}
