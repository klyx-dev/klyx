package com.klyx.terminal

import androidx.compose.runtime.Immutable

@Immutable
data class Cell(val column: Int, val row: Int, val char: Char? = null)
