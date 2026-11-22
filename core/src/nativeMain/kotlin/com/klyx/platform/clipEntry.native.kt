@file:OptIn(ExperimentalComposeUiApi::class)

package com.klyx.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

actual fun clipEntryOfText(text: String) = ClipEntry.withPlainText(text)
