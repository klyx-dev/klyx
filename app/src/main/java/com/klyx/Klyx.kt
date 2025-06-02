package com.klyx

import android.app.Application
import com.klyx.editor.KlyxCodeEditor

class Klyx : Application() {
    override fun onCreate() {
        super.onCreate()
        KlyxCodeEditor.apply {
            loadTSLibrary()
            setupFileProviders(this@Klyx)
            registerDefaultGrammers()
            loadTextMateLanguages("textmate/grammers/grammers.json")
            registerDefaultThemes()
        }
    }
}
