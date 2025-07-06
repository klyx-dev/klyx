package com.klyx.core.theme

import androidx.compose.runtime.compositionLocalOf
import com.klyx.core.noLocalProvidedFor

class KlyxColorScheme {

}

val LocalColorScheme = compositionLocalOf<KlyxColorScheme> {
    noLocalProvidedFor<KlyxColorScheme>()
}

fun ThemeFile.asColorScheme(
    darkMode: Boolean = false,
    themeName: String? = null,
): KlyxColorScheme {
    val theme = themes.firstOrNull {
        it.name == themeName || it.appearance == if (darkMode) Appearance.Dark else Appearance.Light
    } ?: themes.firstOrNull()
    return KlyxColorScheme()
}
