@file:OptIn(ExperimentalComposeUiApi::class)

package com.klyx.platform

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

actual fun clipEntryOfText(text: String) = ClipEntry(StringSelection(text))
