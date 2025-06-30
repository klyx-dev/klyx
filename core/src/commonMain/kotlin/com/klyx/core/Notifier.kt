package com.klyx.core

import com.klyx.core.notification.Notification
import com.klyx.core.notification.NotificationManager
import com.klyx.core.notification.NotificationType

class Notifier(
    private val manager: NotificationManager
) {
    fun notify(notification: Notification) {
        manager.show(notification)
    }

    fun notify(
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) = notifyType(NotificationType.Info, null, message, canUserDismiss, durationMillis, onClick)

    fun notify(
        title: String,
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) = notifyType(NotificationType.Info, title, message, canUserDismiss, durationMillis, onClick)

    fun error(
        message: String,
        title: String? = null,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) = notifyType(NotificationType.Error, title, message, canUserDismiss, durationMillis, onClick)

    fun success(
        message: String,
        title: String? = null,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) = notifyType(NotificationType.Success, title, message, canUserDismiss, durationMillis, onClick)

    fun info(
        message: String,
        title: String? = null,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) = notifyType(NotificationType.Info, title, message, canUserDismiss, durationMillis, onClick)

    fun warning(
        message: String,
        title: String? = null,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) = notifyType(NotificationType.Warning, title, message, canUserDismiss, durationMillis, onClick)

    private fun notifyType(
        type: NotificationType,
        title: String?,
        message: String,
        canUserDismiss: Boolean,
        durationMillis: Long,
        onClick: (() -> Unit)?
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
}
