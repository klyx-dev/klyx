package com.klyx.i18n

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

typealias LanguageTag = String

class I18n<T>(
    private val defaultLanguageTag: LanguageTag,
    private val translations: Map<LanguageTag, T>
) {

    private val mutableState = MutableStateFlow(I18nState(defaultLanguageTag, getStrings(defaultLanguageTag)))

    val state = mutableState.asStateFlow()

    var languageTag: LanguageTag
        get() = mutableState.value.languageTag
        set(value) {
            mutableState.value = I18nState(value, getStrings(value))
        }

    private val LanguageTag.fallback: LanguageTag
        get() = split(FALLBACK_REGEX).first()

    private fun getStrings(languageTag: LanguageTag): T =
        translations[languageTag]
            ?: translations[languageTag.fallback]
            ?: translations[defaultLanguageTag]
            ?: throw I18nException("Strings for language tag $languageTag not found")

    companion object {
        private val FALLBACK_REGEX = Regex("[-_]")
    }
}

@ConsistentCopyVisibility
data class I18nState<T> internal constructor(
    val languageTag: LanguageTag,
    val strings: T
)

class I18nException internal constructor(
    override val message: String
) : RuntimeException()

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class I18nStrings(
    val languageTag: LanguageTag,
    val default: Boolean = false
)
