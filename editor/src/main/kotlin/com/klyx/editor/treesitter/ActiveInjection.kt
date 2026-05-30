package com.klyx.editor.treesitter

import io.github.treesitter.ktreesitter.Tree

class ActiveInjection(
    val languageName: String,
    val startChar: Int,
    val endChar: Int,
    val startByte: Int,
    val tree: Tree,
    val queries: LanguageQueries,
    val profile: BracketProfile
)
