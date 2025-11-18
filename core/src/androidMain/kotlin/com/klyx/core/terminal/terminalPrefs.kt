package com.klyx.core.terminal

import android.content.Context
import androidx.core.content.edit

context(context: Context)
private val prefs
    get() = context.getSharedPreferences("terminal", Context.MODE_PRIVATE)

context(context: Context)
var isTerminalSetupDone: Boolean
    get() = prefs.getBoolean("isTerminalSetupDone", false)
    set(value) {
        prefs.edit { putBoolean("isTerminalSetupDone", value) }
    }

context(context: Context)
var currentUser: String?
    get() = prefs.getString("currentUser", null)
    set(value) {
        prefs.edit { putString("currentUser", value) }
    }
