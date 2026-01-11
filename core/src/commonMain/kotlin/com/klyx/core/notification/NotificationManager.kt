package com.klyx.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.noLocalProvidedFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Immutable
class NotificationManager {
    private val _notifications = mutableStateListOf<Notification>()
    val notifications: List<Notification> = _notifications

    private val _toasts = mutableStateListOf<Toast>()
    val toasts: List<Toast> = _toasts

    fun show(notification: Notification) {
        _notifications.add(notification)
        CoroutineScope(Dispatchers.Main).launch {
            delay(notification.durationMillis)
            dismiss(notification.id)
        }
    }

    fun dismiss(id: NotificationId) {
        _notifications.removeAll { it.id == id }
    }

    fun showToast(toast: Toast) {
        _toasts.add(toast)
        CoroutineScope(Dispatchers.Main).launch {
            delay(toast.durationMillis)
            dismissToast(toast)
        }
    }

    fun dismissToast(toast: Toast) {
        toast.onDismiss()
        _toasts.remove(toast)
    }
}

@Composable
@ReadOnlyComposable
fun rememberNotificationManager() = LocalNotificationManager.current

val LocalNotificationManager = staticCompositionLocalOf<NotificationManager> {
    noLocalProvidedFor<NotificationManager>()
}
