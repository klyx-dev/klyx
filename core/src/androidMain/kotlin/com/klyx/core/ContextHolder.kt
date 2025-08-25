package com.klyx.core

import android.app.Activity
import android.content.Context
import arrow.core.None
import arrow.core.Option
import arrow.core.Some
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * A holder for the application context.
 *
 * This object is used to access the application context from anywhere in the app.
 */
object ContextHolder : KoinComponent {
    val context: Context by inject()

    private var currentActivity: Option<Activity> = None

    fun setCurrentActivity(activity: Activity?) {
        currentActivity = if (activity == null) None else Some(activity)
    }

    fun currentActivityOrNull() = currentActivity.getOrNull()
    fun currentActivity() = requireNotNull(currentActivityOrNull()) { "No activity found" }
}
