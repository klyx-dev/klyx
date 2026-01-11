package com.klyx.core

import androidx.compose.runtime.staticCompositionLocalOf
import com.klyx.core.notification.Notification
import com.klyx.core.notification.NotificationManager
import com.klyx.core.notification.NotificationType
import com.klyx.core.notification.Toast

val LocalNotifier = staticCompositionLocalOf<Notifier> {
    noLocalProvidedFor<Notifier>()
}

class Notifier(private val manager: NotificationManager) {
    fun notify(notification: Notification) {
        manager.show(notification)
    }

    fun notify(
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: ((Notification) -> Unit)? = null
    ) = notifyType(NotificationType.Info, null, message, canUserDismiss, durationMillis, onClick)

    fun notify(
        title: String,
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: ((Notification) -> Unit)? = null
    ) = notifyType(NotificationType.Info, title, message, canUserDismiss, durationMillis, onClick)

    fun error(
        message: String,
        title: String? = null,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: ((Notification) -> Unit)? = null
    ) = notifyType(NotificationType.Error, title, message, canUserDismiss, durationMillis, onClick)

    fun success(
        message: String,
        title: String? = null,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: ((Notification) -> Unit)? = null
    ) = notifyType(NotificationType.Success, title, message, canUserDismiss, durationMillis, onClick)

    fun info(
        message: String,
        title: String? = null,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: ((Notification) -> Unit)? = null
    ) = notifyType(NotificationType.Info, title, message, canUserDismiss, durationMillis, onClick)

    fun warning(
        message: String,
        title: String? = null,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: ((Notification) -> Unit)? = null
    ) = notifyType(NotificationType.Warning, title, message, canUserDismiss, durationMillis, onClick)

    private fun notifyType(
        type: NotificationType,
        title: String?,
        message: String,
        canUserDismiss: Boolean,
        durationMillis: Long,
        onClick: ((Notification) -> Unit)?
    ) {
        notify(
            Notification(
                title = title ?: type.name,
                message = message,
                type = type,
                canUserDismiss = canUserDismiss,
                durationMillis = durationMillis,
                onClick = onClick
            )
        )
    }

    fun toast(
        message: String,
        durationMillis: Long = Toast.LENGTH_SHORT,
        onDismiss: () -> Unit = {}
    ) {
        manager.showToast(
            Toast(
                message = message,
                durationMillis = durationMillis,
                onDismiss = onDismiss
            )
        )
    }
}
