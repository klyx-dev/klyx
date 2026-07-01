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


val App.toastHostState by lazy { ToastHostState() }

val LocalToastHostState = compositionLocalWithComputedDefaultOf {
    LocalApp.currentValue.toastHostState
}

@Stable
class ToastHostState {

    private val mutex = Mutex()

    var currentToastData by mutableStateOf<ToastData?>(null)
        private set

    suspend fun showToast(
        message: String,
        icon: ImageVector? = null,
        duration: ToastDuration = ToastDuration.Short
    ) = showToast(ToastVisualsImpl(message, icon, duration))

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

@Stable
interface ToastData {
    val visuals: ToastVisuals
    fun dismiss()
}

@Stable
interface ToastVisuals {
    val message: String
    val icon: ImageVector?
    val duration: ToastDuration
}

sealed class ToastDuration(val time: kotlin.Long) {
    @Stable
    data object Short : ToastDuration(3500L)

    @Stable
    data object Long : ToastDuration(6500L)

    @Stable
    data class Custom(val durationMillis: kotlin.Long) : ToastDuration(durationMillis)
}


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

suspend fun ToastHostState.showFailureToast(
    message: String,
    icon: ImageVector? = null
) = showToast(
    message = message,
    icon = icon ?: Icons.Outlined.ErrorOutline,
    duration = ToastDuration.Long
)

