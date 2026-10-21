package com.klyx.core.utils

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
actual fun clipEntryOfText(text: String) = ClipEntry.withPlainText(text)

