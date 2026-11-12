package com.klyx.core.init

enum class InitPriority {
    CRITICAL,
    EAGER,
    LAZY
}

interface Initializer {
    val priority: InitPriority

    suspend fun initialize()
}
