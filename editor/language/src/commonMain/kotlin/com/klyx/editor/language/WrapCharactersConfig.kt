package com.klyx.editor.language

import kotlinx.serialization.Serializable

@Serializable
data class WrapCharactersConfig(
    /**
     * Opening token split into a prefix and suffix. The first caret goes
     * after the prefix (i.e., between prefix and suffix).
     */
    val startPrefix: String,
    val startSuffix: String,

    /**
     * Closing token split into a prefix and suffix. The second caret goes
     * after the prefix (i.e., between prefix and suffix).
     */
    val endPrefix: String,
    val endSuffix: String
)
