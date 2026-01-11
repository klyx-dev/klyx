@file:UseSerializers(RegexSerializer::class)

package com.klyx.editor.language

import arrow.core.compareTo
import com.klyx.core.serializers.RegexSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

private val LanguageMatcherComparator = object : Comparator<LanguageMatcher> {
    override fun compare(a: LanguageMatcher, b: LanguageMatcher): Int {
        val suffixCmp = a.pathSuffixes.compareTo(b.pathSuffixes)
        if (suffixCmp != 0) return suffixCmp
        return compareValues(a.firstLinePattern?.pattern, b.firstLinePattern?.pattern)
    }
}

/**
 * @property pathSuffixes Given a list of [LanguageConfig]'s, the language of a file can be determined based on the path extension matching any of the [pathSuffixes].
 * @property firstLinePattern A regex pattern that determines whether the language should be assigned to a file or not.
 */
@Serializable
data class LanguageMatcher(
    val pathSuffixes: List<String> = emptyList(),
    val firstLinePattern: Regex? = null
) : Comparable<LanguageMatcher> {
    override fun compareTo(other: LanguageMatcher) =
        compareValuesBy(this, other, LanguageMatcherComparator) { it }
}
