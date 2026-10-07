package com.klyx

import android.app.Application
import com.klyx.di.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class KlyxApp : Application() {
    override fun onCreate() {
        super.onCreate()

        initKoin {
            androidLogger()
            androidContext(this@KlyxApp)
        }
    }
}
