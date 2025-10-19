package com.klyx.editor.compose.selection

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.intl.LocaleList

// TODO https://youtrack.jetbrains.com/issue/CMP-8344
@Composable
internal actual fun rememberPlatformSelectionBehaviors(
    selectedTextType: SelectedTextType,
    localeList: LocaleList?
): PlatformSelectionBehaviors? = null
