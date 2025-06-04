package com.klyx.core.event

import androidx.compose.ui.input.key.KeyEvent

fun android.view.KeyEvent.toComposeKeyEvent() = KeyEvent(this)
