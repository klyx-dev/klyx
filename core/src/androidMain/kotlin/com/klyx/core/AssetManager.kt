package com.klyx.core

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun AssetManager.readText(path: String) = withContext(Dispatchers.IO) {
    open(path).bufferedReader().use { it.readText() }
}
