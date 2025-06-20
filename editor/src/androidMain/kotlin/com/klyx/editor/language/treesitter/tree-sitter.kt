package com.klyx.editor.language.treesitter

import android.content.Context

fun Context.getQueryScm(language: String, queryName: String) = assets.open("ts/$language/queries/$queryName.scm").bufferedReader().use { it.readText() }
