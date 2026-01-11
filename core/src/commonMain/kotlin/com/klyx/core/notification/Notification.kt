package com.klyx.core.notification

import com.klyx.core.generateId

typealias NotificationId = String

enum class NotificationType {
    Info, Success, Warning, Error
}

data class Notification(
    val message: String,
    val type: NotificationType = NotificationType.Info,
    val title: String = type.name,
    val id: NotificationId = generateId(),
    val durationMillis: Long = 4000L,
    val canUserDismiss: Boolean = true,
    val onClick: ((Notification) -> Unit)? = null
)
