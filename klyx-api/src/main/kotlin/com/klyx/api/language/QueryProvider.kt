package com.klyx.api.language

@JvmDefaultWithoutCompatibility
interface QueryProvider {
    fun highlights(): String
    fun indents(): String? = null
    fun folds(): String? = null
    fun locals(): String? = null
    fun injections(): String? = null
    fun tags(): String? = null
}
