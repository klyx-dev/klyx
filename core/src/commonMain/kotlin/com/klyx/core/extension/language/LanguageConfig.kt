package com.klyx.core.extension.language

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LanguageConfig(
    val name: String,
    val grammar: String,
    @SerialName("path_suffixes")
    val pathSuffixes: Array<String> = emptyArray(),
    @SerialName("line_comments")
    val lineComments: Array<String> = emptyArray(),
    @SerialName("tab_size")
    val tabSize: Int = 4,
    @SerialName("hard_tabs")
    val hardTabs: Boolean = false,
    @SerialName("first_line_pattern")
    val firstLinePattern: String = "",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LanguageConfig

        if (tabSize != other.tabSize) return false
        if (hardTabs != other.hardTabs) return false
        if (name != other.name) return false
        if (grammar != other.grammar) return false
        if (!pathSuffixes.contentEquals(other.pathSuffixes)) return false
        if (!lineComments.contentEquals(other.lineComments)) return false
        if (firstLinePattern != other.firstLinePattern) return false

        return true
    }

    override fun hashCode(): Int {
        var result = tabSize
        result = 31 * result + hardTabs.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + grammar.hashCode()
        result = 31 * result + pathSuffixes.contentHashCode()
        result = 31 * result + lineComments.contentHashCode()
        result = 31 * result + firstLinePattern.hashCode()
        return result
    }
}
