package com.klyx.activities.utils

import android.content.Context
import android.content.Intent
import com.klyx.MainActivity
import com.klyx.core.openActivity

context(context: Context)
fun launchNewWindow() {
    openActivity(
        MainActivity::class,
        flags = Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
    )
}
