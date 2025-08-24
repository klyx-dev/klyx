package com.klyx.ui

import android.app.Activity
import android.content.Context
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.klyx.core.logging.logger

private val logger = logger("[klyx]")

context(context: Context)
fun showDialog(
    message: String,
    title: String = "Info"
) {
    if (context is Activity) {
        MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    } else {
        logger.warn { "Current context is not an Activity context, cannot show dialog." }
    }
}
