package com.klyx.api.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import com.klyx.api.util.extractMessage
import com.klyx.core.App
import com.klyx.core.LocalApp
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume


/**
 * Extension property to access the global [ToastHostState] for the application.
 */
val App.toastHostState by lazy { ToastHostState() }

/**
 * CompositionLocal providing the [ToastHostState] to the Compose hierarchy.
 */
val LocalToastHostState = compositionLocalWithComputedDefaultOf {
    LocalApp.currentValue.toastHostState
}

/**
 * State manager for displaying non-blocking toast notifications in the application.
 */
@Stable
class ToastHostState {

    private val mutex = Mutex()

    /**
     * The data for the toast currently being displayed, or null if no toast is visible.
     */
    var currentToastData by mutableStateOf<ToastData?>(null)
        private set

    /**
     * Displays a toast with the given [message], [icon], and [duration].
     *
     * This is a suspend function that waits until the toast is dismissed (either by
     * timeout or manual user action) before returning.
     */
    suspend fun showToast(
        message: String,
        icon: ImageVector? = null,
        duration: ToastDuration = ToastDuration.Short
    ) = showToast(ToastVisualsImpl(message, icon, duration))

    /**
     * Displays a toast with the provided custom [visuals].
     */
    suspend fun showToast(visuals: ToastVisuals) = mutex.withLock {
        try {
            suspendCancellableCoroutine { continuation ->
                currentToastData = ToastDataImpl(visuals, continuation)
            }
        } finally {
            currentToastData = null
        }
    }

    private class ToastVisualsImpl(
        override val message: String,
        override val icon: ImageVector?,
        override val duration: ToastDuration
    ) : ToastVisuals {

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ToastVisualsImpl

            if (message != other.message) return false
            if (icon != other.icon) return false
            return duration == other.duration
        }

        override fun hashCode(): Int {
            var result = message.hashCode()
            result = 31 * result + icon.hashCode()
            result = 31 * result + duration.hashCode()
            return result
        }
    }

    private class ToastDataImpl(
        override val visuals: ToastVisuals,
        private val continuation: CancellableContinuation<Unit>
    ) : ToastData {

        override fun dismiss() {
            if (continuation.isActive) continuation.resume(Unit)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ToastDataImpl

            if (visuals != other.visuals) return false
            return continuation == other.continuation
        }

        override fun hashCode(): Int {
            var result = visuals.hashCode()
            result = 31 * result + continuation.hashCode()
            return result
        }
    }
}

/**
 * Represents the data for a toast that is currently being displayed.
 */
@Stable
interface ToastData {

    /** The visual configuration of the toast. */
    val visuals: ToastVisuals

    /** Dismisses the toast immediately. */
    fun dismiss()
}

/**
 * Defines the visual appearance and behavior of a toast.
 */
@Stable
interface ToastVisuals {

    /** The message text to display. */
    val message: String

    /** An optional icon to display alongside the message. */
    val icon: ImageVector?

    /** How long the toast should remain visible. */
    val duration: ToastDuration
}

/**
 * Defines standard and custom durations for toast visibility.
 *
 * @property time The duration in milliseconds.
 */
sealed class ToastDuration(val time: kotlin.Long) {

    /** Standard short duration (~3.5 seconds). */
    @Stable
    data object Short : ToastDuration(3500L)

    /** Standard long duration (~6.5 seconds). */
    @Stable
    data object Long : ToastDuration(6500L)

    /** A custom duration specified in [durationMillis]. */
    @Stable
    data class Custom(val durationMillis: kotlin.Long) : ToastDuration(durationMillis)
}

/**
 * Convenience extension to display a failure toast based on a [Throwable].
 *
 * It automatically extracts a user-friendly message and adds an appropriate icon
 * (e.g., a memory icon for [OutOfMemoryError]).
 */
suspend fun ToastHostState.showFailureToast(
    throwable: Throwable
) = showFailureToast(
    message = throwable.extractMessage(),
    icon = if (throwable is OutOfMemoryError) {
        Icons.Outlined.Memory
    } else {
        null
    }
)

/**
 * Convenience extension to display a failure toast with a custom [message] and [icon].
 *
 * This version defaults to [ToastDuration.Long] and an error icon.
 */
suspend fun ToastHostState.showFailureToast(
    message: String,
    icon: ImageVector? = null
) = showToast(
    message = message,
    icon = icon ?: Icons.Outlined.ErrorOutline,
    duration = ToastDuration.Long
)

