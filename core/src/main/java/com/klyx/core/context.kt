package com.klyx.core

import android.content.res.AssetManager

fun AssetManager.readText(path: String) = open(path).bufferedReader().use { it.readText() }
