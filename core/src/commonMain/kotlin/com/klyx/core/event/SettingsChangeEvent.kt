package com.klyx.core.event

import com.klyx.core.settings.AppSettings

data class SettingsChangeEvent(val oldSettings: AppSettings, val newSettings: AppSettings)
