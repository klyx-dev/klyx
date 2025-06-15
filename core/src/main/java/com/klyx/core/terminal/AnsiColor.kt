package com.klyx.core.terminal

import androidx.compose.ui.graphics.Color

object AnsiColor {
    val Black = Color(0xFF000000)
    val Red = Color(0xFFCD0000)
    val Green = Color(0xFF00CD00)
    val Yellow = Color(0xFFCDCD00)
    val Blue = Color(0xFF0000CD)
    val Magenta = Color(0xFFCD00CD)
    val Cyan = Color(0xFF00CDCD)
    val White = Color(0xFFE5E5E5)

    val BrightBlack = Color(0xFF666666)
    val BrightRed = Color(0xFFFF0000)
    val BrightGreen = Color(0xFF00FF00)
    val BrightYellow = Color(0xFFFFFF00)
    val BrightBlue = Color(0xFF0000FF)
    val BrightMagenta = Color(0xFFFF00FF)
    val BrightCyan = Color(0xFF00FFFF)
    val BrightWhite = Color(0xFFFFFFFF)

    val DefaultForeground = White
    val DefaultBackground = Black

    fun fromAnsiCode(code: Int): Color {
        return when (code) {
            30 -> Black
            31 -> Red
            32 -> Green
            33 -> Yellow
            34 -> Blue
            35 -> Magenta
            36 -> Cyan
            37 -> White
            90 -> BrightBlack
            91 -> BrightRed
            92 -> BrightGreen
            93 -> BrightYellow
            94 -> BrightBlue
            95 -> BrightMagenta
            96 -> BrightCyan
            97 -> BrightWhite
            else -> DefaultForeground
        }
    }

    fun fromAnsiBackgroundCode(code: Int): Color {
        return when (code) {
            40 -> Black
            41 -> Red
            42 -> Green
            43 -> Yellow
            44 -> Blue
            45 -> Magenta
            46 -> Cyan
            47 -> White
            100 -> BrightBlack
            101 -> BrightRed
            102 -> BrightGreen
            103 -> BrightYellow
            104 -> BrightBlue
            105 -> BrightMagenta
            106 -> BrightCyan
            107 -> BrightWhite
            else -> DefaultBackground
        }
    }
} 
