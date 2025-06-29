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
        canUserDismiss: Boolean = false
    ) = info(message, canUserDismiss)

    fun notify(
        title: String,
        message: String,
        canUserDismiss: Boolean = false
    ) = info(title, message, canUserDismiss)

    fun error(message: String, canUserDismiss: Boolean = false) {
        notify(Notification(message = message, type = NotificationType.Error, canUserDismiss = canUserDismiss))
    }

    fun error(title: String, message: String, canUserDismiss: Boolean = false) {
        notify(Notification(title = title, message = message, type = NotificationType.Error, canUserDismiss = canUserDismiss))
    }

    fun success(message: String, canUserDismiss: Boolean = false) {
        notify(Notification(message = message, type = NotificationType.Success, canUserDismiss = canUserDismiss))
    }

    fun success(title: String, message: String, canUserDismiss: Boolean = false) {
        notify(Notification(title = title, message = message, type = NotificationType.Success, canUserDismiss = canUserDismiss))
    }

    fun info(message: String, canUserDismiss: Boolean = false) {
        notify(Notification(message = message, type = NotificationType.Info, canUserDismiss = canUserDismiss))
    }

    fun info(title: String, message: String, canUserDismiss: Boolean = false) {
        notify(Notification(title = title, message = message, type = NotificationType.Info, canUserDismiss = canUserDismiss))
    }

    fun warning(message: String, canUserDismiss: Boolean = false) {
        notify(Notification(message = message, type = NotificationType.Warning, canUserDismiss = canUserDismiss))
    }

    fun warning(title: String, message: String, canUserDismiss: Boolean = false) {
        notify(Notification(title = title, message = message, type = NotificationType.Warning, canUserDismiss = canUserDismiss))
    }
}
