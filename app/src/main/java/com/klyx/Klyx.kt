package com.klyx

import android.app.Application
import com.klyx.editor.KlyxCodeEditor

class Klyx : Application() {
    override fun onCreate() {
        super.onCreate()
        KlyxCodeEditor.setupFileProviders(this)
    }
}
