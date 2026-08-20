package com.klyx.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.intl.Locale

@Composable
fun <T> rememberStrings(
    translations: Map<LanguageTag, T>,
    defaultLanguageTag: LanguageTag = "en",
    currentLanguageTag: LanguageTag = Locale.current.toLanguageTag()
): I18n<T> {
    return remember(defaultLanguageTag) {
        I18n(defaultLanguageTag, translations).apply {
            languageTag = currentLanguageTag
        }
    }
}

@Composable
fun <T> ProvideStrings(
    i18n: I18n<T>,
    provider: ProvidableCompositionLocal<T>,
    content: @Composable () -> Unit
) {
    val state by i18n.state.collectAsState()

    CompositionLocalProvider(
        provider provides state.strings,
        content = content
    )
}
