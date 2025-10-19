package com.klyx.editor.compose.selection.contextmenu.internal

import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.textclassifier.TextClassification
import androidx.annotation.RequiresApi

internal const val TAG = "TextClassification"

@RequiresApi(28)
internal object TextClassificationHelperApi28 {
    fun sendPendingIntent(pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= 34) {
            TextClassificationHelper34.sendIntentAllowBackgroundActivityStart(pendingIntent)
        } else {
            pendingIntent.send()
        }
    }

    @Suppress("DEPRECATION")
    fun sendLegacyIntent(context: Context, textClassification: TextClassification) {
        val pendingIntent =
            PendingIntent.getActivity(
                context,
                textClassification.text.hashCode(),
                textClassification.intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        sendPendingIntent(pendingIntent)
    }
}

@Suppress("DEPRECATION")
@RequiresApi(34)
private object TextClassificationHelper34 {
    fun sendIntentAllowBackgroundActivityStart(pendingIntent: PendingIntent) {
        try {
            pendingIntent.send(
                ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                    .toBundle()
            )
        } catch (e: PendingIntent.CanceledException) {
            Log.e(TAG, "error sending pendingIntent: $pendingIntent error: $e")
        }
    }
}
