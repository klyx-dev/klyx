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
    ) = info(message, canUserDismiss,durationMillis, onClick)

    fun notify(
        title: String,
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) = info(title, message, canUserDismiss,durationMillis, onClick)

    fun error(
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) {
        notify(
            Notification(
                message = message,
                type = NotificationType.Error,
                canUserDismiss = canUserDismiss,
                onClick = onClick,
                durationMillis = durationMillis
            )
        )
    }

    fun error(
        title: String,
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) {
        notify(
            Notification(
                title = title,
                message = message,
                type = NotificationType.Error,
                canUserDismiss = canUserDismiss,
                onClick = onClick,
                durationMillis = durationMillis
            )
        )
    }

    fun success(
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) {
        notify(
            Notification(
                message = message,
                type = NotificationType.Success,
                canUserDismiss = canUserDismiss,
                onClick = onClick,
                durationMillis = durationMillis
            )
        )
    }

    fun success(
        title: String,
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) {
        notify(
            Notification(
                title = title,
                message = message,
                type = NotificationType.Success,
                canUserDismiss = canUserDismiss,
                onClick = onClick,
                durationMillis = durationMillis
            )
        )
    }

    fun info(
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) {
        notify(
            Notification(
                message = message,
                type = NotificationType.Info,
                canUserDismiss = canUserDismiss,
                onClick = onClick,
                durationMillis = durationMillis
            )
        )
    }

    fun info(
        title: String,
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) {
        notify(
            Notification(
                title = title,
                message = message,
                type = NotificationType.Info,
                canUserDismiss = canUserDismiss,
                onClick = onClick,
                durationMillis = durationMillis
            )
        )
    }

    fun warning(
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) {
        notify(
            Notification(
                message = message,
                type = NotificationType.Warning,
                canUserDismiss = canUserDismiss,
                onClick = onClick,
                durationMillis = durationMillis
            )
        )
    }

    fun warning(
        title: String,
        message: String,
        canUserDismiss: Boolean = false,
        durationMillis: Long = 4000L,
        onClick: (() -> Unit)? = null
    ) {
        notify(
            Notification(
                title = title,
                message = message,
                type = NotificationType.Warning,
                canUserDismiss = canUserDismiss,
                onClick = onClick,
                durationMillis = durationMillis
            )
        )
    }
}
