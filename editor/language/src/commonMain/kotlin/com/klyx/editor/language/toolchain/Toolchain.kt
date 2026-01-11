package com.klyx.editor.language.toolchain

import com.klyx.editor.language.LanguageName
import kotlinx.serialization.json.JsonElement

/**
 * Represents a single toolchain.
 *
 * @property name User-facing label
 * @property path Absolute path
 * @property asJson Full toolchain data (including language-specific details)
 */
data class Toolchain(
    val name: String,
    val path: String,
    val languageName: LanguageName,
    val asJson: JsonElement
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Toolchain

        if (name != other.name) return false
        if (path != other.path) return false
        if (languageName != other.languageName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + languageName.hashCode()
        return result
    }
}
