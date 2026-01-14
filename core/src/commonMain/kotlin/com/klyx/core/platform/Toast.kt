package com.klyx.core.platform

/**
 * Displays a short-lived, unobtrusive notification (a "toast") to the user.
 * This is an `expect` function, requiring a platform-specific `actual` implementation.
 *
 * @param message The text message to be displayed in the toast.
 * @param duration The duration for which the toast should be visible. Defaults to [ToastDuration.Short].
 * @see ToastDuration
 */
expect fun Platform.showToast(message: String, duration: ToastDuration = ToastDuration.Short)

/**
 * Represents the duration for which a toast message is displayed.
 */
enum class ToastDuration {
    /**
     * Typically a few seconds.
     */
    Short,

    /**
     * A longer duration than [Short].
     */
    Long
}
