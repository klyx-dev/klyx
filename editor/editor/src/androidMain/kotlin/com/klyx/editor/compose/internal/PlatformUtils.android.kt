package com.klyx.editor.compose.internal

import android.annotation.SuppressLint
import android.os.Build

@SuppressLint("ObsoleteSdkInt")
internal actual fun isAutofillAvailable(): Boolean = Build.VERSION.SDK_INT >= 26
