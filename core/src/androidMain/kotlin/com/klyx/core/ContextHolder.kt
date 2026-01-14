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

/**
 * Executes the given [block] with the current activity as its receiver.
 *
 * This function provides a convenient way to access the current activity within a lambda,
 * ensuring that the activity is available. If no activity is currently set in [ContextHolder],
 * this function will throw an [IllegalStateException].
 *
 * Example usage:
 * ```kotlin
 * withCurrentActivity {
 *     // 'this' refers to the current Activity
 *     startActivity(Intent(this, AnotherActivity::class.java))
 *     Toast.makeText(this, "Hello from the current activity!", Toast.LENGTH_SHORT).show()
 * }
 * ```
 *
 * @throws IllegalStateException if [ContextHolder.currentActivity] is null.
 */
@OptIn(ExperimentalContracts::class)
inline fun <R> withCurrentActivity(block: Activity.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ContextHolder.currentActivity().block()
}

/**
 * Executes the given [block] function with the application [Context] as its receiver.
 *
 * This is a convenience inline function for accessing the application context held by [ContextHolder].
 * It ensures that the provided block is executed exactly once, immediately.
 *
 * Example:
 * ```kotlin
 * val appName = withAndroidContext { getString(R.string.app_name) }
 * ```
 */
@OptIn(ExperimentalContracts::class)
inline fun <R> withAndroidContext(block: Context.() -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return ContextHolder.context.block()
}
