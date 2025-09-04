package com.klyx

import com.russhwolf.settings.Settings

object DisclaimerManager {
    private val settings = Settings()

    private const val KEY_ACCEPTED = "disclaimer_accepted"

    fun hasAccepted(): Boolean {
        return settings.getBoolean(KEY_ACCEPTED, false)
    }

    fun setAccepted() {
        settings.putBoolean(KEY_ACCEPTED, true)
    }
}
