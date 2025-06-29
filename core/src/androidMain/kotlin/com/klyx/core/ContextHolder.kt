package com.klyx.core

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

object ContextHolder : KoinComponent {
    val context: Context by inject()
}
