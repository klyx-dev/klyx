@file:JvmName("GoogleFont")

package com.klyx.core

import android.content.Context
import android.graphics.Typeface
import android.os.Handler
import android.os.HandlerThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.core.provider.FontRequest
import androidx.core.provider.FontsContractCompat
import androidx.core.provider.FontsContractCompat.FontRequestCallback
import com.klyx.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun downloadFont(
    name: String,
    context: Context,
    weight: FontWeight = FontWeight.W400,
    style: FontStyle = FontStyle.Normal,
    bestEffort: Boolean = false,
    onFontDownloaded: suspend (Typeface) -> Unit = {}
) {
    val query = createFontRequestQuery(name, weight, style, bestEffort)
    val request = FontRequest(
        "com.google.android.gms.fonts",
        "com.google.android.gms",
        query,
        R.array.com_google_android_gms_fonts_certs
    )

    val thread = HandlerThread("FontDownloadThread").also { it.start() }
    val handler = Handler(thread.looper)

    FontsContractCompat.requestFont(context, request, object : FontRequestCallback() {
        override fun onTypefaceRetrieved(typeface: Typeface) {
            CoroutineScope(handler.asCoroutineDispatcher()).launch {
                withContext(Dispatchers.Main) {
                    onFontDownloaded(typeface)
                }
            }
        }

        override fun onTypefaceRequestFailed(reason: Int) {
            //context.showShortToast("Font download failed")
        }
    }, handler)
}

@Composable
fun rememberTypeface(
    name: String,
    fontWeight: FontWeight = FontWeight.W400,
    fontStyle: FontStyle = FontStyle.Normal
): State<Typeface?> {
    val context = LocalContext.current
    var typeface: Typeface? by remember { mutableStateOf(null) }

    downloadFont(name, context, fontWeight, fontStyle, bestEffort = true) { typeface = it }
    return rememberUpdatedState(typeface)
}
