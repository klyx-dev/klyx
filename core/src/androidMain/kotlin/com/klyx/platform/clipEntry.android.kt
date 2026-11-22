package com.klyx.platform

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun clipEntryOfText(text: String) = ClipEntry(ClipData.newPlainText("text", text))
