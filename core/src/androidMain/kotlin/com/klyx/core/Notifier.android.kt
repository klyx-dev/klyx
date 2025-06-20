package com.klyx.core

import android.content.Context
import android.widget.Toast

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual class Notifier(
    private val context: Context
) {
    actual fun notify(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
