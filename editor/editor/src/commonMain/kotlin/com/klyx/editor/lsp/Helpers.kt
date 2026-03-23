package com.klyx.editor.lsp

import com.klyx.core.app.App
import com.klyx.core.language.languageIdentifiers
import com.klyx.editor.language.LanguageName

internal fun getLanguageIdForLanguage(languageName: LanguageName, cx: App): String? {
    return languageIdentifiers[languageName.value]
}
