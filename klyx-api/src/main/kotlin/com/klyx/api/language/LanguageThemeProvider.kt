package com.klyx.api.language

fun interface LanguageThemeProvider {
    fun getStyleForCapture(captureName: String): CaptureStyle?
}
