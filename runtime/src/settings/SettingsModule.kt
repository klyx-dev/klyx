package com.klyx.runtime.settings

import androidx.datastore.core.DataStore
import org.koin.core.annotation.Singleton

@Singleton
fun settingsDataStore(): DataStore<Settings> = createDataStore()

