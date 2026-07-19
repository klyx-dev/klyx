package com.klyx.api.language

data class LanguageDescriptor(
    val name: String,
    val extensions: List<String>,
    val fileNames: List<String> = emptyList(),
    val languageId: String = name,
    val displayName: String = name,
)
