package com.klyx.api.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast

/**
 * Detects whether the device is using gesture navigation.
 *
 * @return `true` if gesture navigation is active, `false` otherwise.
 */
fun Context.isGestureNavigation(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        // Gesture navigation was introduced in Android 10 (API 29)
        return false
    }

    return try {
        val navigationMode = Settings.Secure.getInt(contentResolver, "navigation_mode")
        navigationMode == 2 // 0 = 3-button, 1 = 2-button, 2 = gesture
    } catch (e: Settings.SettingNotFoundException) {
        e.printStackTrace()
        false
    }
}

/**
 * Opens a [url] in the system's default web browser.
 */
@SuppressLint("UseKtx")
fun openUrl(url: String) = withApplicationContext {
    val uri = try {
        Uri.parse(url)
    } catch (_: Throwable) {
        return@withApplicationContext
    }

    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(applicationContext, "No application found to open this URL", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Shares the given [text] using the Android share sheet.
 */
fun shareText(text: String) = withApplicationContext {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        startActivity(
            Intent
                .createChooser(intent, "Share via")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(
            applicationContext,
            "No application available for sharing",
            Toast.LENGTH_SHORT
        ).show()
    } catch (e: Exception) {
        Toast.makeText(
            applicationContext,
            "Could not share text: ${e.localizedMessage}",
            Toast.LENGTH_LONG
        ).show()
    }
}

