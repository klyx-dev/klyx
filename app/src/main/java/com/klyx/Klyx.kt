package com.klyx

import android.app.Application

class Klyx : Application() {
    companion object {
        private lateinit var instance: Klyx
        val application: Klyx get() = instance
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
