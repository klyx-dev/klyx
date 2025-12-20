package com.klyx

import com.klyx.core.backgroundScope
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

object DisclaimerManager {
    private val settings by lazy { Settings() }

    private const val KEY_ACCEPTED = "disclaimer_accepted"
    private val _accepted = MutableStateFlow<Boolean?>(null)
    val accepted = _accepted.asStateFlow()

    init {
        backgroundScope.launch(Dispatchers.IO) {
            _accepted.update { settings.getBoolean(KEY_ACCEPTED, false) }
        }
    }

    fun accept() {
        backgroundScope.launch(Dispatchers.IO) {
            settings.putBoolean(KEY_ACCEPTED, true)
            _accepted.update { true }
        }
    }
}
