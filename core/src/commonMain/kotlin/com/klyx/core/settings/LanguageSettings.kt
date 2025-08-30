package com.klyx.core.settings

import kotlinx.serialization.Serializable

/**
 * The settings for a particular language.
 *
 * @property tabSize How many columns a tab should occupy.
 */
@Serializable
data class LanguageSettings(
    val tabSize: UInt
)
