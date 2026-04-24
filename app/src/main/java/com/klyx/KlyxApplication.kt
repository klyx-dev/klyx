package com.klyx

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.annotation.KoinApplication
import org.koin.core.logger.Level
import org.koin.plugin.module.dsl.startKoin

@KoinApplication
class KlyxApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin<KlyxApplication> {
            androidLogger(if (BuildConfig.DEBUG) Level.DEBUG else Level.INFO)
            androidContext(this@KlyxApplication)
        }
    }
}
