package com.klyx.core

import android.app.Activity
import android.content.Context
import arrow.core.None
import arrow.core.Option
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

/**
 * A holder for the application context.
 *
 * This object is used to access the application context from anywhere in the app.
 */
object ContextHolder : KoinComponent {
    val context: Context by inject()

    private var currentActivity: Option<Activity> = None

    fun setCurrentActivity(activity: Option<Activity>) {
        currentActivity = activity
    }

    fun currentActivityOrNull() = currentActivity.getOrNull()
    fun currentActivity() = checkNotNull(currentActivityOrNull()) { "No activity found" }
}

@OptIn(ExperimentalContracts::class)
inline fun <R> withCurrentActivity(block: Activity.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ContextHolder.currentActivity().block()
}

@OptIn(ExperimentalContracts::class)
inline fun <R> withAndroidContext(block: Context.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ContextHolder.context.block()
}
