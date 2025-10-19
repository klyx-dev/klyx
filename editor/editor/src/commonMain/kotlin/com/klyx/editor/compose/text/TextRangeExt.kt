package com.klyx.editor.compose.text

import androidx.compose.ui.text.TextRange

fun IntRange.toTextRange() = TextRange(first.coerceAtLeast(0), last.coerceAtLeast(0))
