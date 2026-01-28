package com.klyx.core.renderer

data class FontMetrics(
    /**
     * greatest extent above origin of any glyph bounding box, typically negative; deprecated with variable fonts
     */
    val top: Float,
    /**
     * distance to reserve above baseline, typically negative
     */
    val ascent: Float,
    /**
     * distance to reserve below baseline, typically positive
     */
    val descent: Float,
    /**
     * greatest extent below origin of any glyph bounding box, typically positive; deprecated with variable fonts
     */
    val bottom: Float,
    /**
     * distance to add between lines, typically positive or zero
     */
    val leading: Float
)
