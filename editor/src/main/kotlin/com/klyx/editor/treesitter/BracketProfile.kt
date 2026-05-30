package com.klyx.editor.treesitter

data class BracketProfile(
    val tokenPairs: Map<String, String> = mapOf("(" to ")", "{" to "}", "[" to "]"),
    val structuralPairs: Map<String, String> = emptyMap()
) {
    val reverseTokenPairs = tokenPairs.entries.associate { it.value to it.key }
    val reverseStructuralPairs = structuralPairs.entries.associate { it.value to it.key }

    companion object {
        val DEFAULT = BracketProfile()

        val MARKUP = BracketProfile(
            tokenPairs = mapOf("(" to ")", "{" to "}", "[" to "]"),
            structuralPairs = mapOf("start_tag" to "end_tag", "tag_name" to "tag_name")
        )

        fun forLanguage(languageName: String): BracketProfile {
            return when (languageName) {
                "html", "xml", "xhtml" -> MARKUP
                else -> DEFAULT
            }
        }
    }
}
