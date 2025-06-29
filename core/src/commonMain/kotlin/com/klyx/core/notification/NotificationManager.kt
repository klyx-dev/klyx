package com.klyx.core.notification

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationManager {
    private val _notifications = mutableStateListOf<Notification>()
    val notifications: List<Notification> = _notifications

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
}

@Composable
fun rememberNotificationManager(): NotificationManager {
    return remember { NotificationManager() }
}

