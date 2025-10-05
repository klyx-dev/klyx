package com.klyx.core.settings

import com.kyant.monet.PaletteStyle
import com.russhwolf.settings.Settings

const val SHOW_FPS = "show_fps"

private val settings by lazy { Settings() }

val paletteStyles = listOf(
    PaletteStyle.TonalSpot,
    PaletteStyle.Spritz,
    PaletteStyle.FruitSalad,
    PaletteStyle.Vibrant,
    PaletteStyle.Monochrome,
)

const val STYLE_TONAL_SPOT = 0
const val STYLE_SPRITZ = 1
const val STYLE_FRUIT_SALAD = 2
const val STYLE_VIBRANT = 3
const val STYLE_MONOCHROME = 4

const val THEME_COLOR = "theme_color"
const val PALETTE_STYLE = "palette_style"
