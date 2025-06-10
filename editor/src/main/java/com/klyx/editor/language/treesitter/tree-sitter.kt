package com.klyx.editor.language.treesitter

import android.content.Context
import com.klyx.treesitter.java.TreeSitterJava
import com.klyx.treesitter.json.TreeSitterJson
import com.klyx.treesitter.kotlin.TreeSitterKotlin

fun Context.getQueryScm(language: String, queryName: String) =
    assets.open("ts/$language/queries/$queryName.scm").bufferedReader().use { it.readText() }

fun String.treeSitterLanguage(): Any = when (this) {
    "kotlin" -> TreeSitterKotlin.language()
    "java" -> TreeSitterJava.language()
    "json" -> TreeSitterJson.language()
    else -> throw IllegalArgumentException("Unsupported language: $this")
}
