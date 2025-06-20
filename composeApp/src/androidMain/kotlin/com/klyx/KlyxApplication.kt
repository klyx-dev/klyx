package com.klyx

import android.app.Application
import com.klyx.core.di.initKoin
import com.klyx.extension.ExtensionFactory
import com.klyx.viewmodel.EditorViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

class KlyxApplication : Application() {
    companion object {
        private lateinit var instance: KlyxApplication
        val application: KlyxApplication get() = instance
    }

    override fun onCreate() {
        super.onCreate()
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
