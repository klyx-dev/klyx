package com.klyx.editor.language

import com.klyx.editor.language.treesitter.TSLanguage
import com.klyx.editor.language.treesitter.TSQuery

object PlainLanguage : KlyxLanguage {
    override val language: TSLanguage? get() = null
    override fun getHighlightsQuery(): TSQuery? = null
}
