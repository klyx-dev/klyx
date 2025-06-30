package com.klyx.core

import android.content.Context
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * A holder for the application context.
 *
 * This object is used to access the application context from anywhere in the app.
 */
object ContextHolder : KoinComponent {
    val context: Context by inject()
}
