package com.klyx.core.platform

import android.widget.Toast
import com.klyx.core.withCurrentActivity

actual fun Platform.showToast(
    message: String,
    duration: ToastDuration
) {
    val duration = when (duration) {
        ToastDuration.Short -> Toast.LENGTH_SHORT
        ToastDuration.Long -> Toast.LENGTH_LONG
    }

    withCurrentActivity { Toast.makeText(this, message, duration).show() }
}
